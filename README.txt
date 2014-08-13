http://docs.oracle.com/cd/E35855_01/tuxedo/docs12c/install/inscon.html
Server Install
Password is welcome


$ sudo apt-get update
$ sudo apt-get upgrade
$ sudo apt-get install build-essential


http://docs.oracle.com/cd/E35855_01/tuxedo/docs12c/install/inspin.html#wp1171674

buildclient -o simpcl -f "-Xlinker --no-as-needed simpcl.c"

In the ubbsimple file the machine name should the output of uname -n