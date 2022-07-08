import java.net.*;
import java.io.*;
import java.util.*;
public class TftpClient
{
	private static final byte RRQ = 1;
	private static final byte DATA = 2;
	private static final byte ACK = 3;
	private static final byte ERROR = 4;
	private static byte packetPrev;
	private static Random random =new Random();

	public static void main(String args[]){
		try{
			//making a new datagram socet
			DatagramSocket ds = new DatagramSocket();
			
			//this buf will be used to send the filename, it is 514 size to include teh type, packet num and data
			byte[]buf =new byte[514];
			
			//argument 0 is the lab computer, so it gets ip based on that
			InetAddress IP = InetAddress.getByName(args[0]);
					
			//getting the filename from list of arguments
			String filename=args[2].trim();     
			File file = new File(filename);			
			FileOutputStream outputStream = new FileOutputStream(file);

			//this gets the filename bytes for the filename
			byte[]filenamebytes=filename.getBytes();            	
			byte[]type =new byte[1];
			type[0]=RRQ;
					
			//putting the filename into byte array
			System.arraycopy(type, 0, buf, 0, type.length);  
			System.arraycopy(filenamebytes, 0, buf, type.length, filenamebytes.length);  
					
			//getting the actual length of the array
			int rc=buf.length;

			//args[0] is the port address
			//thi is the first packet (rrq that asks for a file
			DatagramPacket dp = new DatagramPacket(buf, rc, IP, Integer.parseInt(args[1]));
			ds.send(dp);	
			byte[]toWrite=new byte[512];
			byte[]ack =new byte[2];

			//from here on the main focus is receiving
			while(true){
			
				DatagramPacket p = new DatagramPacket(buf, 514);
				ds.receive(p);
				SocketAddress senderSocetAddress = p.getSocketAddress();				
				outputStream.write(p.getData(), 2,p.getLength()-2);
		
				//see if the packet is a data packet
				if(p.getData()[0]==DATA){
				
					if(p.getData()[1]==0){
						packetPrev=p.getData()[1];
					}
						//basically the bigger the number on the right, the lower the chance of an ack being sent.
					if(random.nextInt(9)>-1){
						System.out.println(p.getLength()+" random "+ random.nextInt(9)+" With packet number: "+p.getData()[1]);
						ack[0]=ACK;
						ack[1]=(byte)p.getData()[1];	
						DatagramPacket ACK = new DatagramPacket(ack, 2, senderSocetAddress);
						ds.send(ACK);
					}
					
					if(p.getLength()!=514){						
						System.out.println("Found the last packet, it was: " +p.getLength()+" Long");
						ds.close();
						break;
					}								
				}		
			}
			outputStream.close();
			return;		
		}
		catch(Exception e){
			System.out.println("there was an error:  "+e);
		}
	}
}
