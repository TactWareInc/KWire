package net.tactware.kwire.gradle

import org.gradle.api.file.FileCollection
import java.io.File

/**
 * AnnotationParser
 * -----------------
 * Multi-file, import-aware parser for @RpcService/@RpcMethod Kotlin interfaces.
 * - Resolves simple type names across the whole source set via a symbol index.
 * - Honors explicit imports, wildcard imports, and `import ... as Alias`.
 * - Preserves generics & nullability.
 * - Detects streaming for both Flow<...> and kotlinx.coroutines.flow.Flow<...>.
 */
class AnnotationParser {

    // ------------------------- Public entry points -------------------------

    fun parseServices(files: FileCollection): List<net.tactware.kwire.gradle.ServiceInfo> =
        parseServices(files.files)

    fun parseServices(files: Collection<File>): List<net.tactware.kwire.gradle.ServiceInfo> {
        // Read all files (skip empty/unreadable)
        val texts: Map<File, String> = files.associateWith { f ->
            runCatching { f.readText() }.getOrElse { "" }
        }.filterValues { it.isNotBlank() }

        // Build a project-wide symbol index of top-level types
        val symbolIndex = buildSymbolIndex(texts)

        // Parse each file using its context + the global symbol index
        val out = mutableListOf<ServiceInfo>()
        texts.forEach { (_, content) ->
            val ctx = extractFileContext(content)
            out += parseServices(content, ctx, symbolIndex)
        }
        return out
    }

    // ----------------------------- Regexes ---------------------------------

    private val PACKAGE_RE =
        Regex("""^\s*package\s+([a-zA-Z0-9_.]+)\s*$""", RegexOption.MULTILINE)

    // import line with optional alias:  import com.x.Foo as Bar
    private val IMPORT_RE =
        Regex("""^\s*import\s+([a-zA-Z0-9_.]+)(?:\s+as\s+([A-Za-z_][A-Za-z0-9_]*))?\s*$""",
            RegexOption.MULTILINE)

    private val SERVICE_HEADER_RE =
        Regex("""@RpcService\("([^"]+)"\)\s*interface\s+(\w+)""", RegexOption.MULTILINE)

    // One-line method signatures. Parameters may be multi-line; return type is line-bounded.
    private val METHOD_RE =
        Regex("""@RpcMethod\("([^"]+)"\)\s*(?:suspend\s+)?fun\s+(\w+)\s*\(([^)]*)\)\s*:\s*([^\n{=]+)""",
            RegexOption.MULTILINE)

    // Top-level type declarations for symbol indexing
    private val TYPE_DECL_RE =
        Regex("""^\s*(?:public\s+|internal\s+|private\s+)?(?:data\s+)?(class|interface|enum\s+class|object)\s+(\w+)\b""",
            RegexOption.MULTILINE)

    // ------------------------------- Context --------------------------------

    internal data class FileContext(
        val pkg: String,
        val explicitImports: List<String>,        // e.g., com.example.model.User
        val wildcardImports: List<String>,        // e.g., com.example.model.*
        val importAliases: Map<String, String>    // alias -> FQN
    )

    private fun extractFileContext(content: String): FileContext {
        val pkg = PACKAGE_RE.find(content)?.groupValues?.get(1) ?: ""
        val matches = IMPORT_RE.findAll(content).map { it.groupValues }.toList()

        val explicit = matches
            .mapNotNull { g -> g[1].takeIf { !it.endsWith(".*") } }
            .map { it.trim() }

        val wildcards = matches
            .mapNotNull { g -> g[1].takeIf { it.endsWith(".*") } }
            .map { it.trim() }

        val aliases = matches
            .filter { it[2].isNotBlank() }
            .associate { g -> g[2].trim() to g[1].trim() }

        return FileContext(pkg, explicit, wildcards, aliases)
    }

    // ------------------------------- Parsing --------------------------------

    internal fun parseServices(
        content: String,
        ctx: FileContext,
        symbolIndex: Map<String, Set<String>>
    ): List<ServiceInfo> {
        val services = mutableListOf<ServiceInfo>()
        SERVICE_HEADER_RE.findAll(content).forEach { m ->
            val serviceName = m.groupValues[1].trim()
            val interfaceName = m.groupValues[2].trim()
            val methods = parseMethods(content, ctx, symbolIndex)
            services += ServiceInfo(
                serviceName = serviceName,
                packageName = ctx.pkg,
                interfaceName = interfaceName,
                methods = methods
            )
        }
        return services
    }

    private fun parseMethods(
        content: String,
        ctx: FileContext,
        symbols: Map<String, Set<String>>
    ): List<MethodInfo> {
        val methods = mutableListOf<MethodInfo>()
        for (m in METHOD_RE.findAll(content)) {
            val rpcId     = m.groupValues[1].trim()
            val name      = m.groupValues[2].trim()
            val paramsRaw = m.groupValues[3]
            val retRaw    = m.groupValues[4]

            val params = parseParams(paramsRaw, ctx, symbols)
            val returnType = qualifyType(cleanType(retRaw), ctx, symbols)

            val isStreaming =
                returnType.startsWith("kotlinx.coroutines.flow.Flow<") ||
                        returnType.startsWith("Flow<") // retain true if left unqualified

            methods += MethodInfo(
                rpcMethodId = rpcId,
                methodName = name,
                parameters = params,
                returnType = returnType,
                isStreaming = isStreaming
            )
        }
        return methods
    }

    private fun parseParams(
        block: String,
        ctx: FileContext,
        symbols: Map<String, Set<String>>
    ): List<ParamInfo> {
        val trimmed = block.trim()
        if (trimmed.isEmpty()) return emptyList()

        val parts = splitTopLevelCommas(trimmed)
        return parts.filter { it.isNotBlank() }.map { part ->
            val idx = part.indexOf(':')
            require(idx > 0) { "Bad parameter entry: '$part'" }
            val name = part.substring(0, idx).trim()
            val type = qualifyType(cleanType(part.substring(idx + 1)), ctx, symbols)
            ParamInfo(name = name, type = type)
        }
    }

    // --------------------------- Symbol indexing ----------------------------

    private fun buildSymbolIndex(texts: Map<File, String>): Map<String, Set<String>> {
        val map = linkedMapOf<String, MutableSet<String>>()

        // 1) Scan all files for package + top-level type declarations
        texts.values.forEach { content ->
            val pkg = PACKAGE_RE.find(content)?.groupValues?.get(1) ?: return@forEach
            TYPE_DECL_RE.findAll(content).forEach { m ->
                val simple = m.groupValues[2].trim()
                map.getOrPut(simple) { linkedSetOf() } += "$pkg.$simple"
            }
        }

        // 2) Seed standard library, collections, and Flow
        fun seed(simple: String, fqn: String) {
            map.getOrPut(simple) { linkedSetOf() } += fqn
        }

        listOf("Int","Long","Short","Byte","Float","Double","Boolean","Unit","String")
            .forEach { seed(it, "kotlin.$it") }

        listOf("List","Set","Map","MutableList","MutableSet","MutableMap","Collection","Iterable")
            .forEach { seed(it, "kotlin.collections.$it") }

        seed("Flow", "kotlinx.coroutines.flow.Flow")

        return map
    }

    // ----------------------------- Type helpers -----------------------------

    private fun cleanType(t: String): String =
        t.trim()
            .removeSuffix(";")
            .substringBefore("//")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun qualifyType(
        type: String,
        ctx: FileContext,
        symbols: Map<String, Set<String>>
    ): String {
        // Handle generics & nullability recursively
        val nullable = type.endsWith('?')
        val core = if (nullable) type.dropLast(1) else type
        val lt = core.indexOf('<')

        val qualified = if (lt >= 0) {
            val base = core.substring(0, lt)
            val args = core.substring(lt + 1, core.lastIndexOf('>'))
            val qBase = qualifySimple(base, ctx, symbols)
            val qArgs = splitTopLevelCommas(args).joinToString(", ") { arg ->
                qualifyType(arg.trim(), ctx, symbols)
            }
            "$qBase<$qArgs>"
        } else {
            qualifySimple(core, ctx, symbols)
        }

        return if (nullable) "$qualified?" else qualified
    }

    private fun qualifySimple(
        simpleOrFqn: String,
        ctx: FileContext,
        symbols: Map<String, Set<String>>
    ): String {
        // Already FQN
        if ('.' in simpleOrFqn) return simpleOrFqn

        // 0) alias import (import com.x.User as U)
        ctx.importAliases[simpleOrFqn]?.let { return it }

        // 1) explicit imports
        ctx.explicitImports.firstOrNull { it.endsWith(".$simpleOrFqn") }?.let { return it }

        // 2) wildcard imports (only accept if present in symbol table)
        ctx.wildcardImports.forEach { star ->
            val pkg = star.removeSuffix(".*")
            val fqn = "$pkg.$simpleOrFqn"
            if (symbols[simpleOrFqn]?.contains(fqn) == true) return fqn
        }

        // 3) same package
        val samePkg = if (ctx.pkg.isNotEmpty()) "${ctx.pkg}.$simpleOrFqn" else simpleOrFqn
        if (symbols[simpleOrFqn]?.contains(samePkg) == true) return samePkg

        // 4) global, unambiguous
        symbols[simpleOrFqn]?.let { if (it.size == 1) return it.first() }

        // 5) leave simple; generator can add imports or you can enforce strictness later
        return simpleOrFqn
    }

    // Split by commas not inside matching <...>
    private fun splitTopLevelCommas(s: String): List<String> {
        val out = ArrayList<String>()
        val buf = StringBuilder()
        var depth = 0
        for (ch in s) {
            when (ch) {
                '<' -> { depth++; buf.append(ch) }
                '>' -> { if (depth > 0) depth--; buf.append(ch) }
                ',' -> if (depth == 0) { out += buf.toString().trim(); buf.clear() } else buf.append(ch)
                else -> buf.append(ch)
            }
        }
        if (buf.isNotEmpty()) out += buf.toString().trim()
        return out
    }
}
