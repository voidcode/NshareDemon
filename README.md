NshareDemon
===========

This is a Nshare Demon(Ubuntu). 

This can revitive data/udp-packets from the [Nshare Android app](https://github.com/voidcode/NshareOnAndroid).

If the packet is a url it lunchers your default webbrowser.

Or if it is just a text string it open gedit and add the text to your clipboard(desktop)


Try it.

```
git clone git@github.com:voidcode/NshareDemon.git

cd bin

java -cp ".:gson-2.2.4.jar" NshareDemon
```

This you will get a output like below:

java.io.FileNotFoundException: /home/myuser/Desktop/NshareDemon/bin/blocklist.txt (No such file or directory)
NshareDemon is running on port: 12345


TODO:

1) On showdown kill the thred / app.

2) Make it adds a real demon / bg-service(runs on system-startup). 

3) Make it work / one click install on Window 7.

4) Package it as a .deb and then add to apt-get... (one click install..)




