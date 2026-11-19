package com.symphine.gamespace.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

enum class ShizukuConnectionPhase(val label: String) {
    NOT_INSTALLED("Not Installed"),
    SERVICE_NOT_RUNNING("Service Not Running"),
    PERMISSION_REQUIRED("Permission Required"),
    PERMISSION_DENIED("Permission Revoked / Denied"),
    CONNECTING("Connecting UserService..."),
    CONNECTED("Connected"),
    DISCONNECTED("Disconnected"),
    ERROR("Connection Error")
}

data class ShizukuStatusState(
    val isShizukuAppInstalled: Boolean = false,
    val isBinderAlive: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isUserServiceBound: Boolean = false,
    val phase: ShizukuConnectionPhase = ShizukuConnectionPhase.NOT_INSTALLED,
    val shizukuVersion: Int? = null,
    val serviceUid: Int? = null,
    val lastError: String? = null,
    val errorLogs: List<String> = emptyList()
) {
    val isOperational: Boolean
        get() = isBinderAlive && isPermissionGranted && isUserServiceBound
}

class ShizukuManager(private val context: Context) {

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 9421
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(ShizukuStatusState())
    val state: StateFlow<ShizukuStatusState> = _state.asStateFlow()

    @Volatile
    private var userService: ISymphineShizukuService? = null

    private var onDisconnectCallback: (() -> Unit)? = null

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, SymphineShizukuUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("symphine_shell")
            .tag("SymphineUserService_v1")
            .debuggable(false)
            .version(1)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service == null || !service.pingBinder()) {
                appendError("UserService binder dead on connect")
                _state.update {
                    it.copy(
                        isUserServiceBound = false,
                        phase = ShizukuConnectionPhase.ERROR,
                        lastError = "UserService binder invalid"
                    )
                }
                return
            }
            val stub = ISymphineShizukuService.Stub.asInterface(service)
            userService = stub
            val uid = runCatching { stub.uid }.getOrNull()
            _state.update {
                it.copy(
                    isUserServiceBound = true,
                    serviceUid = uid,
                    phase = ShizukuConnectionPhase.CONNECTED,
                    lastError = null
                )
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            _state.update {
                it.copy(
                    isUserServiceBound = false,
                    serviceUid = null,
                    phase = ShizukuConnectionPhase.DISCONNECTED
                )
            }
            onDisconnectCallback?.invoke()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatus(autoBindIfPermitted = true)
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        userService = null
        appendError("Shizuku binder died")
        _state.update {
            it.copy(
                isBinderAlive = false,
                isUserServiceBound = false,
                serviceUid = null,
                phase = ShizukuConnectionPhase.DISCONNECTED,
                lastError = "Shizuku service stopped or disconnected"
            )
        }
        onDisconnectCallback?.invoke()
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            if (granted) {
                refreshStatus(autoBindIfPermitted = true)
            } else {
                appendError("Shizuku permission denied by user")
                _state.update {
                    it.copy(
                        isPermissionGranted = false,
                        phase = ShizukuConnectionPhase.PERMISSION_DENIED,
                        lastError = "Permission denied"
                    )
                }
            }
        }
    }

    fun initialize(onDisconnected: (() -> Unit)? = null) {
        onDisconnectCallback = onDisconnected
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }.onFailure {
            appendError("Failed to register Shizuku listeners: ${it.message}")
        }
        refreshStatus(autoBindIfPermitted = true)
    }

    fun refreshStatus(autoBindIfPermitted: Boolean = false) {
        val installed = isShizukuInstalled()
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

        if (!binderAlive) {
            userService = null
            _state.update {
                it.copy(
                    isShizukuAppInstalled = installed,
                    isBinderAlive = false,
                    isPermissionGranted = false,
                    isUserServiceBound = false,
                    serviceUid = null,
                    shizukuVersion = null,
                    phase = if (installed) ShizukuConnectionPhase.SERVICE_NOT_RUNNING else ShizukuConnectionPhase.NOT_INSTALLED
                )
            }
            return
        }

        val version = runCatching { Shizuku.getVersion() }.getOrNull()
        val preV11 = runCatching { Shizuku.isPreV11() }.getOrDefault(false)
        if (preV11) {
            appendError("Shizuku version < v11 is unsupported")
            _state.update {
                it.copy(
                    isShizukuAppInstalled = installed,
                    isBinderAlive = true,
                    shizukuVersion = version,
                    phase = ShizukuConnectionPhase.ERROR,
                    lastError = "Unsupported Shizuku version (< v11)"
                )
            }
            return
        }

        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

        if (!granted) {
            _state.update {
                it.copy(
                    isShizukuAppInstalled = installed,
                    isBinderAlive = true,
                    isPermissionGranted = false,
                    isUserServiceBound = false,
                    shizukuVersion = version,
                    phase = ShizukuConnectionPhase.PERMISSION_REQUIRED
                )
            }
            return
        }

        val currentlyBound = userService?.asBinder()?.pingBinder() == true
        _state.update {
            it.copy(
                isShizukuAppInstalled = installed,
                isBinderAlive = true,
                isPermissionGranted = true,
                isUserServiceBound = currentlyBound,
                shizukuVersion = version,
                phase = if (currentlyBound) ShizukuConnectionPhase.CONNECTED else ShizukuConnectionPhase.CONNECTING
            )
        }

        if (!currentlyBound && autoBindIfPermitted) {
            bindUserService()
        }
    }

    fun requestPermission() {
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!binderAlive) {
            appendError("Cannot request permission: Shizuku service not running")
            refreshStatus(autoBindIfPermitted = false)
            return
        }
        runCatching {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                refreshStatus(autoBindIfPermitted = true)
            } else {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            }
        }.onFailure {
            appendError("Permission request error: ${it.message}")
            _state.update { st ->
                st.copy(phase = ShizukuConnectionPhase.ERROR, lastError = it.message)
            }
        }
    }

    fun bindUserService() {
        runCatching {
            if (!Shizuku.pingBinder()) {
                refreshStatus(false)
                return
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                refreshStatus(false)
                return
            }
            _state.update { it.copy(phase = ShizukuConnectionPhase.CONNECTING, lastError = null) }
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        }.onFailure {
            appendError("bindUserService failed: ${it.message}")
            _state.update { st ->
                st.copy(
                    isUserServiceBound = false,
                    phase = ShizukuConnectionPhase.ERROR,
                    lastError = it.message
                )
            }
        }
    }

    fun reconnect() {
        unbindUserService()
        refreshStatus(autoBindIfPermitted = true)
    }

    fun unbindUserService() {
        runCatching {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        }
        userService = null
        _state.update {
            it.copy(
                isUserServiceBound = false,
                serviceUid = null,
                phase = if (it.isBinderAlive) ShizukuConnectionPhase.DISCONNECTED else it.phase
            )
        }
    }

    fun getServiceOrNull(): ISymphineShizukuService? {
        val svc = userService
        return if (svc != null && svc.asBinder().pingBinder()) svc else null
    }

    fun release() {
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        }
        unbindUserService()
    }

    private fun isShizukuInstalled(): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            }
            true
        }.getOrDefault(false)
    }

    private fun appendError(msg: String) {
        scope.launch {
            _state.update { st ->
                st.copy(errorLogs = (listOf(msg) + st.errorLogs).take(30))
            }
        }
    }
}
