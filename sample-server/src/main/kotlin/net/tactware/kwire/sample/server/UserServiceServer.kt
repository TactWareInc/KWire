package net.tactware.kwire.sample.server

import net.tactware.kwire.core.RpcServer
import net.tactware.kwire.sample.api.UserService

@RpcServer
abstract class UserServiceServer(impl : UserService)