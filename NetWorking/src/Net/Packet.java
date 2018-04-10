package Net;

public class Packet {

    private boolean synPacket;
    private boolean ackPacket;
    private boolean dataPacket;
    private int synNum;
    private int ackNum;
    private String data;
    private int sequence_num;

    public Packet(){
        this.synPacket = false;
        this.ackPacket = false;
        this.synNum = 0;
        this.ackNum = 0;
        this.data = null;
        this.sequence_num = 0;
        this.dataPacket = false;
    }

    public Packet(boolean synPacket,boolean ackPacket,int synNum,int ackNum,String data,int sequence_num,boolean dataPacket){
        this.synPacket = synPacket;
        this.ackPacket = ackPacket;
        this.synNum = synNum;
        this.ackNum = ackNum;
        this.data = data;
        this.sequence_num = sequence_num;
        this.dataPacket = dataPacket;
    }

    public boolean isDataPacket() {
        return dataPacket;
    }

    public int getSequence_num() {
        return sequence_num;
    }

    public boolean getSynPacket() {
        return synPacket;
    }

    public boolean getAckPacket() {
        return ackPacket;
    }

    public int getAckNum() {
        return ackNum;
    }

    public int getSynNum() {
        return synNum;
    }

    public String getData() {
        return data;
    }

    public void setAckPacket(boolean ackPacket) {
        this.ackPacket = ackPacket;
    }

    public void setSynPacket(boolean synPacket) {
        this.synPacket = synPacket;
    }

    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
    }

    public void setSynNum(int synNum) {
        this.synNum = synNum;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setSequence_num(int sequence_num) {
        this.sequence_num = sequence_num;
    }

    public void setDataPacket(boolean dataPacket) {
        this.dataPacket = dataPacket;
    }

    @Override
    public String toString(){//to send a packet we must convert it to a String first
        int syn,ack,dataN;
        if(synPacket){
            syn = 1;
        }else{
            syn = 0;
        }

        if(ackPacket){
            ack = 1;
        }else{
            ack = 0;
        }

        if(dataPacket){
            dataN=1;
        }else{
            dataN = 0;
        }

        int sNum = synNum;
        int aNum = ackNum;
        int seq_num = sequence_num;
        String dataT = data;

        String msg = "SYN=" + syn + "-" + "ACK=" + ack + "-" + "SYNn=" + sNum + "-" + "ACKn=" + aNum + "-" + "DATA=" +
                dataT + "-" +"SEQ=" + seq_num + "-" + "Dpacket=" + dataN + "-";
        return msg;
    }

    public static Packet processingData(String packetData){

        boolean synPacket;
        boolean ackPacket;
        int synNum;
        int ackNum;
        String data = "";

        String[] tokens = packetData.split("-");
        Packet packet = new Packet();
        for(String eachToken: tokens){
            String Tokens[] = eachToken.split("=");
            switch (Tokens[0]){
                case "SYN":
                    packet.setSynPacket(Tokens[1].equals("1"));
                    break;
                case "ACK":
                    packet.setAckPacket(Tokens[1].equals("1"));
                    break;
                case "SYNn":
                    packet.setSynNum(Integer.parseInt(Tokens[1]));
                    break;
                case "ACKn":
                    packet.setAckNum(Integer.parseInt(Tokens[1]));
                    break;
                case "DATA":
                    packet.setData(Tokens[1]);
                    break;
                case "SEQ":
                    packet.setSequence_num(Integer.parseInt(Tokens[1]));
                    break;
                case "Dpacket":
                    packet.setDataPacket(Tokens[1].equals("1"));
                    System.out.println(Tokens[1]);
                    break;
            }
        }

        return packet;
    }
}
