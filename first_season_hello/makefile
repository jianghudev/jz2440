hello:hello.o a.o
	gcc -o hello hello.c a.c
hello.o:hello.c
	gcc -o hello.o -c hello.c
a.o:a.c
	gcc -o a.o -c a.c
clean:
	rm -rf hello *.o
