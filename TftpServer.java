import java.net.*;
import java.io.*;
import java.util.*;

class TftpServerWorker extends Thread
{
	//declaring useful variables
	private DatagramPacket req;
	private static final byte RRQ = 1;
	private static final byte DATA = 2;
	private static final byte ACK = 3;
	private static final byte ERROR = 4;

	boolean finalPacket=false;//true if the last packet is 512 long 
	
	int missedPackets=0;
	
    private void sendfile(String filename)
    {
        /*
         * open the file using a FileInputStream and send it, one block at
         * a time, to the receiver.
         */
		try{

		//Declaring a datagram socket, and getting the socket address of the req packet sent by client
		//these will be used to send packets back to the client.
		DatagramSocket ds = new DatagramSocket();	 
		ds.setSoTimeout(1000);
		SocketAddress senderSocetAddress = req.getSocketAddress();
		
		//bufis the total bytes that will be sent to the client, this includes the 2 header bytes for
		//type of packet as well as the blovk number
		byte[] buf = new byte[514];//512
		
		//readIn is the amount of data read in from a file in one go to be sent to the client.
		byte[] readIn = new byte[512];//510
		
		//Length of data to be sent
		int length=buf.length;
		
		//Input stream to read in files (including text, images)
		FileInputStream fis = new FileInputStream(filename);
		
		//packetnum starts at 0, increments with each packet sent
		int packetNum=1;
		
		//type is a small array that stores the type of packet, as well as the packet num
		byte[]type =new byte[2];
		
		//Declaring more variables.
		int actualLength;
		DatagramPacket reply;
		
		System.out.println("Sent, then recieved format [Packet Type], [Packet Number]");
		
		while(missedPackets<5){

			//Loop while there is information to be read in, this also stores a maximum of 512 bytes in the readIn array and keeps
			//track of how many bytes were actually stored.
			while((actualLength=fis.read(readIn))!=-1){	

				try{

					//sets up the type array, we already have the readIn array, we can then use arraycopy to store
					//the result of their "addition" into a another array, called buf			
					type[0]=DATA;
					type[1]=(byte)packetNum;						
					System.arraycopy(type, 0, buf, 0, type.length);  
					System.arraycopy(readIn, 0, buf, type.length,actualLength );  //readIn.length
						
					//uses the data we have generated, as well as the socketAddress of the sender to send the packet.
					reply= new DatagramPacket(buf, actualLength+2, senderSocetAddress);					
					ds.send(reply);

					//sleep helps to see each packet going in succession, which looks visually nice			
					sleep(5);
					
					//Increments the packetNum, then uses a variety of system outputs to visually show if there is a missed packet
					//if the last packet was exactly 512 long we need to send another empty packet
					if(reply.getLength()==514){
						finalPacket=true;
					}
					else{				
						finalPacket=false;
					}							
					System.out.println(reply.getData()[0] + " "+reply.getData()[1]);
					ds.receive(reply);					
					System.out.println(reply.getData()[0] + " "+reply.getData()[1]);					
					missedPackets=0;					
				}
				catch(Exception e){
					missedPackets++;
					System.out.println(e+Integer.toString(missedPackets));
					break;					
				}
			packetNum++;								
			}
			if(fis.read(readIn)==-1){break;}
		}
		
		//shows that all the packets have been sent.
		System.out.println("exitted loop serverside");
		
		if(finalPacket){
			packetNum++;
			byte[]empty =new byte[2];
			empty[0]=DATA;
			empty[1]=(byte)packetNum;
			reply= new DatagramPacket(empty, empty.length, senderSocetAddress);	
			ds.send(reply);
			System.out.println("the final packet was 512 long");
			System.out.println("Sent an extra packet");
		}	
	}
	catch(Exception e){	
		System.out.println(e);
	}        
	return;
    }

    public void run(){
        /*
         * parse the request packet, ensuring that it is a RRQ
         * and then call sendfile
         */
         try{

         	//Datagram socket created and the socket address of sender stored, for communication.
         	DatagramSocket ds = new DatagramSocket();	    
			InetAddress addressSender=req.getAddress();
		
			//experimenting with get port.
			int senderPort = req.getPort();

			//No real reason to store the data, as getData returns a byte array anyway, mainly for experimentation
			byte[] recievedStuff = new byte[514];
			recievedStuff=req.getData();

			//visual feedback
			System.out.println(recievedStuff[0]+"   Recieved from adress: "+addressSender+" From Senderport: "+senderPort);
			
			//Gets the "string" of the filename the client is requesting.
			String requestedFileName = new String(recievedStuff);
			requestedFileName=requestedFileName.trim();	
			System.out.println("the client requested a file with the name :"+requestedFileName);
			
			//Turns it into a file so that I have access to . file.exists.
			File requestedFile = new File(requestedFileName); 
			
			//checking to see if the wanted file is here
			if(requestedFile.exists()){
				//if found, start sending the file
				System.out.println("The file was found");					
				sendfile(requestedFileName);
			}
			else{
				System.out.println("File not Found");
			}		
		}
		catch(Exception e){
			System.out.println("There was an error: "+e);
		}
    }
    public TftpServerWorker(DatagramPacket req){
		this.req = req;
    }
}

class TftpServer
{
    public void start_server(){
		try {
			DatagramSocket ds = new DatagramSocket();//given code
			System.out.println("TftpServer on port " + ds.getLocalPort());
			
			while(true){//starts the server, and allocates a "worker" for each new client.
			byte[] buf = new byte[1472];
			DatagramPacket p = new DatagramPacket(buf, 1472);
			ds.receive(p);		
			
			TftpServerWorker worker = new TftpServerWorker(p);
			worker.start();
			}
		}
		catch(Exception e) {
			System.err.println("Exception: " + e);
		}

		return;
    }

    public static void main(String args[]){
		TftpServer d = new TftpServer();
		d.start_server();
    }
}

