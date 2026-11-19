package com.symphine.gamespace.device

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.symphine.gamespace.core.model.StorageHardwareInfo
import java.io.File

class StorageInfoProvider(private val context: Context) {

    fun getStorageInfo(): StorageHardwareInfo {
        val dataDir = Environment.getDataDirectory()
        val statFs = StatFs(dataDir.path)
        val total = statFs.totalBytes.coerceAtLeast(0L)
        val available = statFs.availableBytes.coerceAtLeast(0L)
        val used = (total - available).coerceAtLeast(0L)

        val cacheBytes = directorySize(context.cacheDir) + directorySize(context.codeCacheDir)
        val appDataBytes = directorySize(context.filesDir)

        return StorageHardwareInfo(
            totalBytes = total,
            availableBytes = available,
            usedBytes = used,
            appCacheBytes = cacheBytes,
            appDataBytes = appDataBytes
        )
    }

    fun clearAppCacheOnly(): Long {
        val before = directorySize(context.cacheDir)
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        val after = directorySize(context.cacheDir)
        return (before - after).coerceAtLeast(0L)
    }

    private fun directorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) size += file.length()
        }
        return size
    }
}
