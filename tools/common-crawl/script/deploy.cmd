@ECHO OFF

::Path to key
set key-folder=%DEV_TOOLS%\keys
set key-name=private.key.ppk

::Set cluster name every time a new instance is created
set remote-name=%1

:: Destination folder
set dst-folder=%2
set src-folder=%3
set file-name=%4

@echo [info] File to deploy: %src-folder%/%file-name%
@echo [info] Host name: %remote-name%
@echo [info] Attempting do deploy %file-name% to %remote-name%:%dst-folder%%file-name%

@echo pscp -i %key-folder%\%key-name% %src-folder%/%file-name% %remote-name%:%dst-folder%%file-name%
pscp -i %key-folder%\%key-name% %src-folder%/%file-name% %remote-name%:%dst-folder%%file-name%