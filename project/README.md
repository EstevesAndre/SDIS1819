### Compile commands:
1. Linux or MacOS:
``` 
$ ./compile.sh 
```
### Run commands inside bin folder:
1. Start RMI registry 
``` 
$ rmiregistry 
```
2. Start a peer
``` 
$ java project/service/Peer 1.0 1234 RemoteInterface "230.0.0.0 9876" "230.0.0.1 9877" "230.0.0.2 9878"
``` 
3. Start the Test Application
``` 
$ java project/service/TestApp "localhost RemoteInterface" BACKUP testFile.txt 2
``` 
