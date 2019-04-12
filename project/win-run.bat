@echo off

SET nPeers=%1
CD ./bin

FOR /l %%x IN (1, 1, %nPeers%) DO (
    START cmd.exe /k "java project/service/Peer 1.0 %%x RemoteInterface "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878""
)
PAUSE