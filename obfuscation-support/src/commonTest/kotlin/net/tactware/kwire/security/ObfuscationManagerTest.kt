package net.tactware.kwire.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for ObfuscationManager.
 */
class ObfuscationManagerTest {
    
    @Test
    fun testObfuscationManagerCreation() {
        val config = ObfuscationConfig(
            enabled = true,
            strategy = ObfuscationStrategy.HASH_BASED,
            methodNameLength = 8
        )
        val manager = ObfuscationManager(config)
        
        assertTrue(manager.isObfuscationEnabled())
    }
    
    @Test
    fun testGenerateMappings() {
        val manager = ObfuscationManager()
        val services = listOf(
            ServiceInfo(
                name = "TestService",
                methods = listOf("method1", "method2", "method3")
            )
        )
        
        val mappings = manager.generateMappings(services)
        
        assertNotNull(mappings)
        assertTrue(mappings.services.containsKey("TestService"))
        assertEquals(3, mappings.services["TestService"]?.size)
        
        // Verify all methods are mapped
        val testServiceMethods = mappings.services["TestService"]!!
        assertTrue(testServiceMethods.containsKey("method1"))
        assertTrue(testServiceMethods.containsKey("method2"))
        assertTrue(testServiceMethods.containsKey("method3"))
        
        // Verify obfuscated names are different from original
        testServiceMethods.forEach { (original, obfuscated) ->
            assertTrue(original != obfuscated)
            assertTrue(obfuscated.isNotEmpty())
        }
    }
    
    @Test
    fun testMethodNameObfuscation() {
        val manager = ObfuscationManager()
        val services = listOf(
            ServiceInfo(
                name = "TestService",
                methods = listOf("testMethod")
            )
        )
        
        manager.generateMappings(services)
        
        val obfuscatedName = manager.getObfuscatedMethodName("TestService", "testMethod")
        assertNotNull(obfuscatedName)
        assertTrue(obfuscatedName != "testMethod")
        
        val resolved = manager.getOriginalMethodName(obfuscatedName)
        assertNotNull(resolved)
        assertEquals("TestService", resolved.first)
        assertEquals("testMethod", resolved.second)
    }
    
    @Test
    fun testServiceNameObfuscation() {
        val config = ObfuscationConfig(
            enabled = true,
            obfuscateServiceNames = true
        )
        val manager = ObfuscationManager(config)
        val services = listOf(
            ServiceInfo(
                name = "TestService",
                methods = listOf("testMethod")
            )
        )
        
        manager.generateMappings(services)
        
        val obfuscatedServiceName = manager.getObfuscatedServiceName("TestService")
        assertNotNull(obfuscatedServiceName)
        // With service name obfuscation enabled, should be different
        assertTrue(obfuscatedServiceName != "TestService")
        
        val originalServiceName = manager.getOriginalServiceName(obfuscatedServiceName)
        assertEquals("TestService", originalServiceName)
    }
    
    @Test
    fun testMappingsExportImport() {
        val manager = ObfuscationManager()
        val services = listOf(
            ServiceInfo(
                name = "TestService",
                methods = listOf("method1", "method2")
            )
        )
        
        val originalMappings = manager.generateMappings(services)
        val exportedJson = manager.exportMappings()
        
        assertNotNull(exportedJson)
        assertTrue(exportedJson.contains("TestService"))
        assertTrue(exportedJson.contains("method1"))
        assertTrue(exportedJson.contains("method2"))
        
        // Create new manager and load mappings
        val newManager = ObfuscationManager()
        newManager.loadMappings(exportedJson)
        
        // Verify mappings are loaded correctly
        val obfuscatedMethod1 = manager.getObfuscatedMethodName("TestService", "method1")
        val newObfuscatedMethod1 = newManager.getObfuscatedMethodName("TestService", "method1")
        assertEquals(obfuscatedMethod1, newObfuscatedMethod1)
    }
    
    @Test
    fun testDifferentObfuscationStrategies() {
        val strategies = listOf(
            ObfuscationStrategy.RANDOM,
            ObfuscationStrategy.HASH_BASED,
            ObfuscationStrategy.SEQUENTIAL
        )
        
        strategies.forEach { strategy ->
            val config = ObfuscationConfig(strategy = strategy)
            val manager = ObfuscationManager(config)
            val services = listOf(
                ServiceInfo(
                    name = "TestService",
                    methods = listOf("testMethod")
                )
            )
            
            val mappings = manager.generateMappings(services)
            val obfuscatedName = mappings.services["TestService"]?.get("testMethod")
            
            assertNotNull(obfuscatedName, "Strategy $strategy should generate obfuscated name")
            assertTrue(obfuscatedName != "testMethod", "Strategy $strategy should obfuscate method name")
        }
    }
    
    @Test
    fun testObfuscationDisabled() {
        val config = ObfuscationConfig(enabled = false)
        val manager = ObfuscationManager(config)
        
        assertFalse(manager.isObfuscationEnabled())
        
        // When obfuscation is disabled, should return original names
        val obfuscatedMethod = manager.getObfuscatedMethodName("TestService", "testMethod")
        assertEquals("testMethod", obfuscatedMethod)
        
        val obfuscatedService = manager.getObfuscatedServiceName("TestService")
        assertEquals("TestService", obfuscatedService)
    }
    
    @Test
    fun testMappingValidation() {
        val validMappings = ObfuscationMappings(
            services = mapOf(
                "Service1" to mapOf("method1" to "m1", "method2" to "m2"),
                "Service2" to mapOf("method3" to "m3", "method4" to "m4")
            )
        )
        
        assertTrue(ObfuscationUtils.validateMappings(validMappings))
        
        // Test invalid mappings with duplicates
        val invalidMappings = ObfuscationMappings(
            services = mapOf(
                "Service1" to mapOf("method1" to "m1", "method2" to "m1"), // Duplicate obfuscated name
                "Service2" to mapOf("method3" to "m3")
            )
        )
        
        assertFalse(ObfuscationUtils.validateMappings(invalidMappings))
    }
}

