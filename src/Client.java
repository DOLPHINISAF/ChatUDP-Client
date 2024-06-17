
public class Client {
    public static void main(String[] args) {
        Server server = new Server();



        server.Conectare();

        server.Close();
    }
}
