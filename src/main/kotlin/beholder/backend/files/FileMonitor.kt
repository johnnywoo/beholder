package beholder.backend.files

import beholder.backend.config.Configuration
import beholder.backend.BadStartException
import java.nio.file.Paths
import java.nio.file.Path
import java.util.HashSet
import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds

class FileMonitor(val conf: Configuration) {
    var onFileFound: (file: String) -> Unit = {}
    var onFileLost: (file: String) -> Unit = {}
    var onLineScanned: (file: String, text: String) -> Unit = {file, text ->}

    fun startInBackground() {
        if (conf.app.logFileMasks.size == 0) {
            throw BadStartException("Please set up logFileMasks array in ${conf.configDir}/app.json")
        }

        thread.start()
    }

    private val thread = object : Thread() {
        private val masks = conf.app.logFileMasks.copyOf()
        private val rootPath = Paths.get("/")!!
        private val watchService = FileSystems.getDefault()?.newWatchService()!!

        override fun run() {
            findNewFiles()

            while (true) {
                waitForEventsAndProcess()
            }
        }

        private fun waitForEventsAndProcess() {
            val watchKey = watchService.take() // take() is blocking

            val events = watchKey.pollEvents()
            if (events != null) {
                for (event in events) {
                    when (event.kind()) {
                        StandardWatchEventKinds.ENTRY_CREATE -> {
                            findNewFiles() // TODO this will not work for /home/*/logs/error.log
                        }
                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            val dir = watchKey.watchable() as Path
                            val file = event.context() as Path
                            val fullPath = dir.resolve(file)
                            fileChanged(fullPath)
                        }
                        StandardWatchEventKinds.ENTRY_DELETE -> {
                            pruneFiles()
                        }
                    }
                }
            }

            if (!watchKey.reset()) {
                watchKey.cancel();
                println("Cancelled: " + watchKey.watchable().javaClass.getName() + " = " + watchKey.watchable())
            }
        }


        //
        // WATCHING
        //

        private fun startWatching(file: Path) {
            onFileFound(file.toString()) // TODO we need to run this on the main thread, not in the child

            file.getParent()?.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )
        }

        private fun fileChanged(file: Path) {
            if (watchedFiles.contains(file)) {
                // TODO we need to run this on the main thread, not in the child
                onLineScanned(file.toString(), "new size " + file.toFile().length()) // TODO detect all this
            }
        }

        private fun stopWatching(file: Path) {
            onFileLost(file.toString()) // TODO we need to run this on the main thread, not in the child
        }


        //
        // FILE MASKS
        //

        private val watchedFiles = HashSet<Path>()

        private fun getAbsolutizedMask(mask: String): String {
            if (mask.startsWith("/")) {
                return mask
            }
            return conf.configDir + "/" + mask
        }

        private fun pruneFiles() {
            for (path in watchedFiles.copyToArray()) {
                if (!path.toFile().exists()) {
                    watchedFiles.remove(path)
                    stopWatching(path)
                }
            }
        }

        private fun findNewFiles() {
            for (mask in masks) {
                collectFilesRecursively(watchedFiles, rootPath, getAbsolutizedMask(mask).substring(1))
            }
        }

        private fun collectFilesRecursively(files: MutableSet<Path>, path: Path, glob: String) {
            val currentLevelGlob = glob.substringBefore("/")
            val deeperGlob = glob.substringAfter("/")
            Files.newDirectoryStream(path, currentLevelGlob)?.use {
                for (childPath in it) {
                    if (childPath.toFile().isDirectory()) {
                        collectFilesRecursively(files, childPath, deeperGlob)
                    } else {
                        if (files.add(childPath)) {
                            startWatching(childPath)
                        }
                    }
                }
            }
        }
    }
}
