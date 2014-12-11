import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.*;
import java.util.*;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
//import iterators.Iterators;
import java.sql.*;

import com.google.gson.Gson;

public class NshareDemon{
	private static String TAG = "NshareDemon";
	private static String rootfolder = System.getProperty("user.dir");
	private static ArrayList<String> blocklist = new ArrayList<String>();
	private static int port = 12345; //default-port
 	private static TreeMap<String, Long> spamList = new TreeMap<String, Long>();
	private static long spamFilterBlockTime = 15000L;
	private static TreeMap<String, String> dataQueueTree = new TreeMap<String, String>();
	private static Gson gson = new Gson(); 
	protected static DatagramSocket ds = null;
	protected static DatagramPacket dp=null;
	public static OS = System.getProperty("os.name").toLowerCase();
	public static void main(String[] args)
	{
		//set default-port via arguments
		/*if(args[0] != null)
		{
			int p = Integer.valueOf(args[0]);
			//only registered ports are those from 1024 through 49151
			if(p > 1023 && p < 49152)
				port = p;
		}*/

		//read 'blocklist.txt' into arraylist 
		loadBlocklist();
		
		Thread th = new Thread(new Runnable() {
			private String packetIP;
			private String packetData;
			@Override
			public void run() {
				try 
				{
					ds = new DatagramSocket(port);
					System.out.println("NshareDemon is running on port: "+port);
					while(true)
					{
						byte[] buffer = new byte[64];
						DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
						try {
							ds.receive(dp);
						} catch (IOException e) {
							e.printStackTrace();
						}
						//retive dest-ip from packet
						packetIP = dp.getAddress().toString().substring(1);
						Long now = System.currentTimeMillis();
						for(Map.Entry<String, Long> entry :spamList.entrySet())
						{
							//this allow frist packet to receivar then, 
							//on spam, only remove 'ip' from 'spamList' after 15sec
							if(now > (entry.getValue() + spamFilterBlockTime)) 
								spamList.remove( entry.getKey() );
						}

						if(!blocklist.contains(packetIP) && !spamList.containsKey(packetIP)) //drop packet if is comes from an tmp-blocked 'packetIP' 
						{
							dataQueueTree.put(packetIP, packetData);
							saveDataQueueToFile();
							spamList.put(packetIP, System.currentTimeMillis()); //add current time
							//retive data from packet
							packetData = new String(dp.getData()).trim();
							//open with default-browser and notify
							if(packetData.startsWith("http"))
							{
								notifySend(packetIP, packetData);
								luncherBrowser(packetData);
							}
							else //only notify and copy-text-to-desktop-clipholder
							{
								notifySend("Copy text from "+packetIP, packetData);
								copyToClipBoard(packetData);
								if(OS.indexOf("win") > 0)
								exec(new String[]{"gedit", "From-"+packetIP, packetData});
							}
						}
						else
						{
							System.out.println("IP >> "+packetIP+" is blocked!");
						}
					}
				} catch (SocketException e) 
				{
					e.printStackTrace();
				}
			}
		});
    	th.start();
	}
	private static void saveDataQueueToFile()
	{	
		try {
			File file = new File(rootfolder+"/nshare-data.json");
			if(!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(gson.toJson(dataQueueTree));
			bw.close();
			System.out.println(TAG+": "+gson.toJson(dataQueueTree));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static ArrayList<String> loadBlocklist()
	{
		try {
			blocklist.clear();
			BufferedReader br = new BufferedReader(new FileReader(rootfolder+"/blocklist.txt"));
			String line = br.readLine();
			while(line != null)
			{
				blocklist.add(line.trim());
				System.out.println("[blocklist] >> "+line);
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			System.out.println("ERROR:\n"+e.toString());
		}
		return blocklist;
	}
	private static void SpamFilter(String ip)
	{
		
		// added only 'ip' if not is in 'spamList'
	}
	
	private static void luncherBrowser(String url)
	{
		String[] cmd;	
		switch(OS){
			case "unix":
				cmd = {"xdg-open", url}
				break;
			case "win":
				cmd = {"cmd", "explorer", url};
				break;			
		}

		try {		
			Runtime.getRuntime().exec(cmd);
		}
		catch (Exception e)
    	{
        	System.err.print( e.getMessage() );
    	}	
	}
	private static void exec(String[] cmd)
	{
		try {		
			Runtime.getRuntime().exec(cmd);
		}
		catch (Exception e)
    	{
        	System.err.print( e.getMessage() );
    	}	
	}
	private static void copyToClipBoard(String text)
	{
		StringSelection selection = new StringSelection(text);
    	Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    	clipboard.setContents(selection, selection);
	}
	private static void notifySend(String title, String text)
	{
		String icon = rootfolder+"/images/ic_launcher.png";
		String[] cmd;		
		switch(OS){
			case "unix":
				cmd = { "/usr/bin/notify-send", title, text, "-i", icon, "-t", "10000"};
		
				break;
			case "win":
				cmd = {""};
				break;			
		}

		//TODO detecter Win, Mac or Ubuntu? Then Nofity(title, text)
		try {		
			Runtime.getRuntime().exec(cmd);
		}
		catch (Exception e)
    	{
        	System.err.print( e.getMessage() );
    	}	
	}
}
