@echo off

SET v=%1
SET nPeers=%2

FOR /l %%x IN (1, 1, %nPeers%) DO (
    CD ../bin
    START cmd.exe /k "java project/service/Peer %v% %%x RemoteInterface '230.0.0.0 9876' '230.0.0.1 9877' '230.0.0.2 9878'"
)
PAUSE