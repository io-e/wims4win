#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

int nbs[6];

int alreadythere(int n, int i)
{
   int j;
   for (j = 0; j < i; j++)
      if (nbs[j] == n)
         return 1;
   return 0;
}

void generate()
{
   int i;
   int res;

   for (i = 0; i < 6; i++)
   {
      do
      {
         switch(res = rand() % 14)
         {
            case 0:
               res = 100;
               break;
            case 11:
               res = 75;
               break;
            case 12:
               res = 50;
               break;
            case 13:
               res = 25;
               break;
            default:;
         }
      }
      while (alreadythere(res, i));
      nbs[i] = res;
   }
   printf("%d %d %d %d %d %d %d\n", rand() % 900 + 100, nbs[0], nbs[1], nbs[2], nbs[3], nbs[4], nbs[5]);
}

int main(int argc, char ** argv)
{
   int i, nb;

   if (argc != 2)
   {
      printf("Wrong number of arguments. Specify the number of sets of data to generate.\n");
      return 1;
   }

   srand(getpid());
   nb = atoi(argv[1]);
   for (i = 0 ; i < nb; i++)
      generate();
   return 0;
}
