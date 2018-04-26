package Net;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/*
* Server Class: Η κλάση αυτή είναι ο Server μας με τον οποίο κάνουν connect όλοι οι clients.
* O server ζητάει απο τον χρήστη να του δώσει την ip και ένα Port πάνω στα οποία θα μπορεί ο εκάστοτε client να συνδεθεί
* και αφου ολοκληρωθεί η σύνδεση μεταξύ των 2 ξεκινάει το 3 way handshake.
* */


public class Server {

   private String ServerIP;
   private int portNumber;
   private DatagramSocket socket = null;
   private String state = "none";//changes based on the state of the communication!!!
   private int SYNC_NUM = 0,sequence_num=0;
   private int defaultPayload =1024;
   private int maxPayload;
   private boolean hasValue = false;
   private int i = 1;

    private void initialize(String args[]) throws IOException {//initializes the server..asks for ip and port
        System.out.println("Server started!!!");
        if(args.length == 2){
            ServerIP = args[0];
            portNumber = Integer.parseInt(args[1]);

        }else if(args.length == 0){
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Give Server IP: ");
            ServerIP = br.readLine();
            System.out.println("Give Port Number: ");
            portNumber = Integer.parseInt(br.readLine());

        }else{
            System.out.println("Wrong arguments!!!");
        }
        communicationHandler(state);

    }

    /*
    It will handle the communication with the client..receives a packet and waits for a certain amount of time
    before it sents back an ack.
    Η communicationHandler είναι η μέθοδος που χειρίζεται το connection με τον client. Λαμβάνει πακέτο και περιμένει λίγο
    πρωτού στείλει ack πίσω.
     */
    private void communicationHandler(String state){
        try {
            socket = new DatagramSocket(portNumber);
        }catch (SocketException e) {
            e.printStackTrace();
        }
        FileOutputStream file = null;

        while(true){
            byte[] buf = new byte[defaultPayload];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            System.out.println("Waiting for a request!!!");
            try {
                //Λαμβάνει πακέτο και μέσω της Proccesing data Που έχουμε στην Packet
                // αναλύουμε το πακέτο στα επιμέρους στοιχεία του
                socket.receive(packet);
                System.out.println("Got a request!!!");
                Packet p = Packet.processingData(new String(buf));
                if(!hasValue) {
                    maxPayload = Integer.parseInt(p.getData());//we received the payload that it will be used in
                    hasValue = true;                          //the communication
                }
                InetAddress clientAddress = packet.getAddress();//gets the address and the port of the client.
                int clientPort = packet.getPort();
                if(state.equals("none")){//Περιμένουμε πακέτο για να ξεκινήσει το handshake
                    if(p.getSynPacket()){//Τσεκάρουμε αν το πακέτο που λάβαμε είναι syn_packet
                        System.out.println("Client initialized 3-way handshake!!!");
                        Packet response = new Packet();
                        response.setSynPacket(true);
                        response.setAckPacket(true);
                        response.setSynNum(SYNC_NUM);
                        response.setAckNum(p.getSynNum());
                        sendData(response.toString(),clientAddress,clientPort);
                        state = "syn_received";//Σε περίπτωση που πράγματι είναι syn το πακέτο στέλνουμε πίσω
                                               // στον client ενα syn_ack για να τον ενημερωσουμε ότι λάβαμε το syn packet.
                        // Για να το επιτύχουμε αυτό θέτουμε τα πεδία SynPacket Και AckPacket σε true του packet που στέλνουμε
                        // και αλλάζουμε το state σε syn_received
                        System.out.println("3-way handshake step 2: sent ack-syn packet waiting for ack...");
                    }
                }else if(state.equals("syn_received")){//εδώ ελέγχουμε αν όντως έχουμε λάβει syn packet
                    if(p.getAckPacket() && p.getAckNum() == SYNC_NUM){ //Τσεκάρουμε αν αυτό που λάβαμε είναι ack packet και αν
                        // πράγματι είναι τότε κάνουμε το state established και έχουμε εγκαθιδρύσει το 3 way handshake.
                        state = "established";
                        System.out.println("3-way handshake completed...CONNECTION ESTABLISHED!!!");
                        try {
                            file = new FileOutputStream("FTPresult" + i);//Το τελικό αρχείο που προκύπτει
                        }catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }else if(state.equals("established")){//όταν πια έχω ολοκληρώσει το 3 way με επιτυχία
                    //Αν το πακέτο είναι έγκυρο..ανοίγω το αρχείο και το μετατρέπω σε byteArray ώστε να το στείλουμε στον client.
                    boolean completed = false;
                    while(!completed) {//γίνεται μέχρι να λάβει όλα τα πακέτα ουσιαστικά μέχρι να παραληφθεί όλο το αρχείο
                        byte[] buffer = new byte[maxPayload];//buffer μεγέθους payload που δίνει ο χρήστης και λαμβάνει ένα πακέτο την φορά
                        DatagramPacket packet1 = new DatagramPacket(buffer, buffer.length);
                        try {
                            socket.setSoTimeout(0);
                            socket.receive(packet1);
                            ByteArrayInputStream inputStream = new ByteArrayInputStream(packet1.getData());
                            DataInputStream in = new DataInputStream(inputStream);
                            int sequence_number = in.readInt();
                            if(sequence_number == sequence_num){
                                int packetLength = in.readInt();
                                byte[] data = new byte[packetLength];
                                in.read(data);
                                file.write(data);
                                causeDelay();
                                sendAck(sequence_number,clientAddress,clientPort);//στέλνουμε ack ότι λάβαμε το πακέτο πίσω στον client
                                if(sequence_num == 1){//αλλάζουμε το seq_number
                                    sequence_num--;
                                }else if(sequence_num == 0){
                                    sequence_num++;
                                }
                            }else if(sequence_number==2){//το seq_number γίνεται 2 μόνο όταν έχει ολοκληρωθεί η μεταφορά του αρχείου
                                System.out.println("File has been transferred!!");
                                int packetLength = in.readInt();
                                byte[] data = new byte[packetLength];
                                in.read(data);
                                file.write(data);
                                causeDelay();
                                sendAck(sequence_number,clientAddress,clientPort);
                                completed = true;
                                in.close();
                                inputStream.close();
                                file.close();
                                completed = true;
                                hasValue = false;
                                state="none";
                                i++;
                                sequence_num = 0;
                            }else{
                                System.out.println("Packet is a duplicate...just send ack!!!");//σε περίπτωση που έχουμε λάβει το πακέτο
                                // αλλά δεν έχουμε στείλει ack. Οπότε το πακέτο που λάβαμε είναι duplicate και στέλνουμε πίσω μόνο το ack του.
                                causeDelay();
                                sendAck(sequence_number,clientAddress,clientPort);
                            }
                        } catch (IOException | InterruptedException e) {
                             e.printStackTrace();

                        }
                    }

                }
            } catch (IOException e) {
                System.out.println("No requests have been made...");
            }
        }

    }

    //sendAck: Την χρησιμοποιούμε για να στείλουμε ack για το πακέτο που μόλις παραλάβαμε μέσω της sendData
    private void sendAck(int ackNum,InetAddress address,int clientPort) throws IOException {
        Packet ack = new Packet();
        ack.setAckPacket(true);
        ack.setAckNum(ackNum);
        sendData(ack.toString(),address,clientPort);
    }

    //sendData: Την χρησιμοποιούμε προκειμένου να στείλουμε data στον client μέσω ενός DatagramPacket
    private void sendData(String data,InetAddress address,int clientPort) throws IOException {
        byte[] buffer = data.getBytes();
        DatagramPacket p = new DatagramPacket(buffer,buffer.length,address,clientPort);
        socket.send(p);
    }

    //causeDelay: η διαδικασία
    //παραγωγής επιβεβαίωσης με την εισαγωγή τεχνητής καθυστέρησης, η οποία ακολουθεί
    //εκεθετικής κατανομή
    private void causeDelay() throws InterruptedException {
      double timeout = getTimeout();
      long timeoutTime = (long) timeout;
      Thread.sleep(timeoutTime);
    }
    //getTimeout: υπολογισμός του τεχνητού Timeout
    private double getTimeout() {
      Random rand = new Random();
      double lambda = 1/4.2;
      return  Math.log(1-rand.nextDouble())/(-lambda);
    }


    public static void main(String args[]) throws IOException {
      new Server().initialize(args);
    }


}
