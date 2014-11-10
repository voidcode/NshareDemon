import java.awt.EventQueue;

import java.awt.datatransfer.*;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.DefaultListModel;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.UIManager;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.java.ayatana.ApplicationMenu;
import org.java.ayatana.AyatanaDesktop;

import com.google.gson.Gson;
import java.awt.Color;
import java.awt.FlowLayout;


public class NshareDesktop extends JFrame {
	private String rootfolder = System.getProperty("user.dir");
	private Gson gson = new Gson();
	private BufferedReader br;
	private JOptionPane jOptionPane = new JOptionPane();
	private TreeMap<String, String> dataQueueTree = new TreeMap<String, String>();
	private JList list;
	private ArrayList<String> bbc = new ArrayList<String>();
	private static DefaultListModel<String> dataQueue = new DefaultListModel<String>();
	private static int port = 12345; //default-port
 	private static TreeMap<String, Long> spamList = new TreeMap<String, Long>();
	private static long spamFilterBlockTime = 15000L;
	private ArrayList<String> blocklist = new ArrayList<String>();
	private String icon = rootfolder+File.separatorChar+"images"+File.separatorChar+"ic_launcher.png";
	/**
	 * Launch the application.
	 */
	protected String newPacketIP=null;
	protected String newPacketData=null;
	protected DatagramSocket ds = null;
	protected DatagramPacket dp=null;
	private JTextArea textarea;

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setTitle("Nshare-desktop");
		setBounds(0, 0, 400, 360);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		loadBlocklist();
		getContentPane().setLayout(new GridLayout(0, 1, 0, 0));
		list = new JList(dataQueue);
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg) {
				if(!arg.getValueIsAdjusting())
				{
					JList jl = (JList) arg.getSource();
					if(jl.getSelectedValue() != null)
					{	
						final String data = jl.getSelectedValue().toString();
						if(data.contains("http"))
						{
							notifySend("Starter", data, "4000");
							luncherBrowser(data);
							dataQueue.remove(jl.getSelectedIndex());
						}
						else
						{
							notifySend("Saved in clipboard", data, "1000");
							copyToClipBoard(data);
							dataQueue.remove(jl.getSelectedIndex());
						}
					}
				}
			}
		});
		getContentPane().add(list);
		textarea = new JTextArea();
		textarea.setBorder(new EmptyBorder(5, 5, 5, 5));
		textarea.setForeground(Color.WHITE);
		textarea.setBackground(Color.DARK_GRAY);
		textarea.setText("Write something ...");
		textarea.setCaretColor(Color.WHITE); 
		textarea.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void mouseExited(MouseEvent arg0) {
			}
			@Override
			public void mouseEntered(MouseEvent arg0) {
			}
			@Override
			public void mouseClicked(MouseEvent arg0) {
				textarea.setText("");
			}
		});
		getContentPane().add(textarea);
		
		//setup socket
		try {
			ds = new DatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void StartDataSniffer()
	{
		Thread th = new Thread(new Runnable() {
			private String packetIP;
			private String packetData;
			@Override
			public void run() {
					System.out.println("Nshare-Desktop is running on port: "+port);
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
							//retive data from packet
							packetData = new String(dp.getData()).trim();
							dataQueue.addElement(packetData);
							spamList.put(packetIP, System.currentTimeMillis()); //add current time
						}
						else
						{
							System.out.println("IP >> "+packetIP+" is blocked!");
						}
					}
			}
		});
    	th.start();
	}
	public void SetupMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		JMenu doMenu = new JMenu("Do?");
		menuBar.add(doMenu);
		JMenuItem clearqueueAction = new JMenuItem("Clear");
		JMenuItem exitAction = new JMenuItem("Exit");
		JMenuItem sendpacketAction = new JMenuItem("Send packet");
		JMenuItem editblocklistAction = new JMenuItem("Edit blocklist");
		doMenu.add(editblocklistAction);
		doMenu.add(sendpacketAction);
		doMenu.addSeparator();
		doMenu.add(clearqueueAction);
		
		doMenu.add(exitAction);
		exitAction.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		sendpacketAction.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String data = textarea.getText();
				if(data.length() <0)
				{
					SendPacket("192.168.1.37", data);
					notifySend("Packet is send to 192.168.1.37", data, "10000");
				}
				else
					notifySend("Can´t send an empty packet", "You need to write someting ...", "10000");
			}
		});
		clearqueueAction.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataQueue.clear();
			}
		});
		editblocklistAction.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg) {
				try {		
					notifySend("Edit", "blocklist.txt", "2000");
					Runtime.getRuntime().exec(new String[]{"gedit", rootfolder+File.separatorChar+"blocklist.txt"});
				}
				catch (Exception e)
		    	{
		        	System.err.print( e.getMessage() );
		    	}	
			}
		});
	}
	private void loadBlocklist()
	{
		try {
			blocklist.clear();
			File f = new File(rootfolder+File.separatorChar+"blocklist.txt");
			if(!f.exists())
				f.createNewFile();
			BufferedReader br = new BufferedReader(new FileReader(f));
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
	}
	private void luncherBrowser(String url)
	{
		String[] cmd = {"xdg-open", url};
		try {		
			Runtime.getRuntime().exec(cmd);
		}
		catch (Exception e)
    	{
        	System.err.print( e.getMessage() );
    	}	
	}
	private void notifySend(String title, String text, String ttl)
	{
		//TODO detecter Win, Mac or Ubuntu? Then Nofity(title, text)
		String[] cmd = { "/usr/bin/notify-send", title, text, "-i", icon, "-t", ttl};
		try {		
			Runtime.getRuntime().exec(cmd);
 
		}
		catch (Exception e)
    	{
        	System.err.print( e.getMessage() );
    	}	
	}
	private void copyToClipBoard(String text)
	{
		StringSelection stringSelection = new StringSelection (text);
		Clipboard clpbrd = Toolkit.getDefaultToolkit ().getSystemClipboard ();
		clpbrd.setContents (stringSelection, null);
	}
	private void SendPacket(final String ip, final String data)
	{
		Thread sendpacketThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					dp = new DatagramPacket(data.getBytes(), data.length(), InetAddress.getByName(ip), port);				
					ds.send(dp);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		sendpacketThread.start();
	}
	/**
	 * Create the application.
	 */
	public NshareDesktop() {
		//set default-platform GUI look and feel
		try { 
		    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		    e.printStackTrace();
		}
		initialize();
		SetupMenuBar();
		StartDataSniffer();// <-- no-ui-thread
	}
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					NshareDesktop window = new NshareDesktop();
					if (AyatanaDesktop.isSupported())
					{ 
						ApplicationMenu.tryInstall(window);
					}
					window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					window.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
