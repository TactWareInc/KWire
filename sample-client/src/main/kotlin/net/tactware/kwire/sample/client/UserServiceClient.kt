package net.tactware.kwire.sample.client

import net.tactware.kwire.core.RpcClient
import net.tactware.kwire.sample.api.UserService

@RpcClient(service = "UserServiceClient")
abstract class UserServiceClient : UserService{
}