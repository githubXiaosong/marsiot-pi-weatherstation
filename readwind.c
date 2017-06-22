// to compile: gcc i2c-analog.c -o i2c-analog -lncurses
#include <stdio.h>
#include <fcntl.h>
#include <math.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>


// SCL -->  SCL1(PIN 05)
// SDA -->  SDA1(PIN 03)
// AIN0 <-- SIGNAL
// PI'GND connect to WIND'GND, so signal is ok else error


// Reverse the data; i.e. 255 becomes 0.
// I did this because it didn't make sense
// to me to have 255 = complete darkness
int rev(int val)
{
	return abs(val - 255);
}

int main(int argc, char **argv)
{
	int r;
	int fd;
	unsigned char value[4];
	useconds_t delay = 2000;
	char *dev = "/dev/i2c-1";
	int addr = 0x48;
	int i, j, val_mod, cnt, flag;

	flag = 0;

	/*
	 * 100
	 */
	cnt = 0;
	for (i = 0; i < 128; i++)
	{
		val_mod = i % 100;
		if (val_mod == 0 || flag == 1)
		{
			if (flag != 1)
			{
				if (cnt != 0)
				{
					flag = 1;
				}
				cnt++;
			}
			else
			{
				j = i % 10;
				if (j == 0 || j == 5) {
					//addch('1');
				}
			}
		}
	}
	/*
	 * 10
	 */
	cnt = 0;
	for (i = 0; i < 128; i++)
	{
		val_mod = i % 10;
		//move(3, i + 12);
		if (val_mod == 0)
		{
			if (cnt != 0)
			{
			}
			cnt++;
		}
		else if (i % 5 == 0 && i > 10)
		{
			/*if (i > 10 && i < 20) addch('1');
			if (i > 20 && i < 30) addch('2');
			if (i > 30 && i < 40) addch('3');
			if (i > 40 && i < 50) addch('4');
			if (i > 50 && i < 60) addch('5');
			if (i > 60 && i < 70) addch('6');
			if (i > 70 && i < 80) addch('7');
			if (i > 80 && i < 90) addch('8');
			if (i > 90 && i < 100) addch('9');
			if (i > 100 && i < 110) addch('0');
			if (i > 110 && i < 120) addch('1');
			if (i > 120 && i < 130) addch('2');*/
		}
	}
	/*
	 * 0-9
	 */
	for (i = 0; i < 128; i++)
	{
		val_mod = i % 10;
		//move(4, i + 12);
		if (val_mod == 0 || val_mod == 5) {
			//printw("%d", val_mod);
        }
	}
	//refresh();
	fd = open(dev, O_RDWR);
	if (fd < 0)
	{
		perror("Opening i2c device node\n");
		return 1;
	}
    //printf("open ok \n");
	r = ioctl(fd, I2C_SLAVE, addr);
	if (r < 0) perror("Selecting i2c device\n");
	//mvaddstr(5, 11, "[");
	//mvaddstr(5, 140, "]");

    int try = 0;
	while (1)
	{
		usleep(delay);
		// the read is always one step behind the selected input
		r = read(fd, &value[0], 1);
		if (r != 1) perror("reading i2c device\n");
        try++;
        if (try > 3) {
            printf("%d \n", value[0]);
            break;
        }

		/*new_val = rev(value[0]);
		new_val /= 2;
		usleep(delay);
		//move(5, 12);
		for (i = 0; i < 128; i++)
		{
			//if (i < new_val) addch('*');
			//else addch(' ');
		}
		//refresh();
		key = getch();
		if (key > -1) break;*/
	}
	//endwin();
	close(fd);
	return(0);
}
