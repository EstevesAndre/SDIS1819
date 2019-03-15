package project;

public class TestClient {
    public static void main(String[] args) {
        if(args.length != 4){
            System.out.println("Wrong number of arguments.\nUsage: java project/TestClient <peer_ap> <sub_protocol> <opnd_1> <opnd_2> ");
        }
    }
}