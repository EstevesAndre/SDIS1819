### Compile commands: (inside project folder)
1. Linux or MacOS:
``` 
$ ./compile.sh 
```
2. Windows:
```
$ ./win-compile.bat
```

### Run commands: (inside bin folder)
1. Start RMI registry 
``` 
$ rmiregistry 
```
2. Start a peer
```
usage: java project/service/Peer <version> <server_id> <RMI_Interface> \"<MC_address> <MC_port>\" \"<MDB_address> <MDB_port>\" \"<MDR_address> <MDR_port>\"
```
```
$ java project/service/Peer 1.0 123 RemoteInterface "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
$ java project/service/Peer 1.0 444 RemoteInterface2 "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
$ java project/service/Peer 1.0 555 RemoteInterface3 "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
$ java project/service/Peer 1.0 666 RemoteInterface4 "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"        
``` 
3. Start the Test Application
``` 
usage: java project/service/TestApp <peer_ap> <sub_protocol> <opnd_1>? <opnd_2>?
```
```
$ java project/service/TestApp "localhost RemoteInterface" BACKUP ./file_test1.txt 2
$ java project/service/TestApp "localhost RemoteInterface" DELETE ./file_test1.txt
$ java project/service/TestApp "localhost RemoteInterface" RESTORE file_test1.txt
$ java project/service/TestApp "localhost RemoteInterface2" RECLAIM 0
```
#### Scripts (inside project folder)
4. Linux:
```
usage: ./linux-run.sh <version> <number_of_peers>)
```
```
./linux-run.sh 1.0 2
```
5. Windows:
```
usage: ./windows-run.bat <version> <number_of_peers>
```
```
./windows-run.bat 1.0 2
```
