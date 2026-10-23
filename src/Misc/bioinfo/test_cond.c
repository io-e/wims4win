#include<stdio.h>
#include<string.h>

/* Entrees :
	argv[1] = nom du prog
	argv[2] = \a
	argv[3] = \b
	argv[4] = \c
	argv[5] = \comparaison1
	argv[6] = \vrai1
	argv[7] = \faux1
	argv[8] = \branchement1
	argv[9] = \comparaison2
	argv[10]= \vrai2
	argv[11]= \faux2
	argv[12]= \branchement2
	argv[13]= \comparaison3
	argv[14]= \confparm1
*/
int main(int argc,char *argv[])
{
	int a;
	int b;
	int c;
	int vrai1;
	int faux1;
	int branchement1;
	int vrai2;
	int faux2;
	int branchement2;
	int niveau;
	char comparaison1[7];
	char comparaison2[7];
	char comparaison3[7];

	sscanf(argv[2],"%d",&a);
	sscanf(argv[3],"%d",&b);
	sscanf(argv[4],"%d",&c);
	strcpy(comparaison1,argv[5]);
	sscanf(argv[6],"%d",&vrai1);
	sscanf(argv[7],"%d",&faux1);
	sscanf(argv[8],"%d",&branchement1);
	strcpy(comparaison2,argv[9]);
	sscanf(argv[10],"%d",&vrai2);
	sscanf(argv[11],"%d",&faux2);
	sscanf(argv[12],"%d",&branchement2);
	strcpy(comparaison3,argv[13]);
	sscanf(argv[14],"%d",&niveau);

	int cmp1 = ((strcmp(comparaison1,"&#62")==0) ? 0 : -1) ;
	int cmp2 = ((strcmp(comparaison2,"&#60")==0) ? 0 : 1) ;
	int cmp3 = ((strcmp(comparaison3,"&#60")==0) ? 0 : 1) ;

	if (niveau==1)
	{
		if (b>a+cmp1)
			b=b+vrai1;
	}

	else if (niveau==2)
	{
		if (b>a+cmp1)
			b=b+vrai1;
		else
			a=a+faux1;
	}

	else if (niveau==3)
	{
		if (b>a+cmp1)
		{
			b=b+vrai1;
			if (branchement1==1)
			{
				if (a<c+cmp2 && b<c+cmp2)
					c=c-vrai2;
			}
		}
		else
		{
			a=a+faux1;
			if (branchement1==0)
			{
				if (a<c+cmp2 && b<c+cmp2)
					c=c-vrai2;
			}

		}
	}


	else if (niveau==4)
	{
		if (b>a+cmp1)
		{
			b=b+vrai1;
			if (branchement1==1)
			{
				if (a<c+cmp2 && b<c+cmp2)
					c=c-vrai2;
				else
				{
					c=c+faux2;
					a++;
				}
			}
		}
		else
		{
			a=a+faux1;
			if (branchement1==0)
			{
				if (a<c+cmp2 && b<c+cmp2)
					c=c-vrai2;
				else
				{
					c=c+faux2;
					a++;
				}

			}

		}
	}


	else if (niveau==5)
	{
		if (b>a+cmp1)
		{
			b=b+vrai1;
			if (branchement1==1)
			{
				if (a<c+cmp2 && b<c+cmp2)
				{
					c=c-vrai2;
					if (branchement2==1)
					{
						if ( a+b<c+cmp3 || b-a < cmp3+c-b )
						{
							a++;
							b++;
						}
					}
				}
				else
				{
					c=c+faux2;
					a++;
					if (branchement2==0)
					{
						if ( a+b<c+cmp3 || b-a < cmp3+c-b )
						{
							a++;
							b++;
						}
					}
				}
			}
		}
		else
		{
			a=a+faux1;
			if (branchement1==0)
			{
				if (a<c+cmp2 && b<c+cmp2)
				{
					c=c-vrai2;
					if (branchement2==1)
					{
						if ( a+b<c+cmp3 || b-a < cmp3+c-b )
						{
							a++;
							b++;
						}
					}
				}
				else
				{
					c=c+faux2;
					a++;
					if (branchement2==0)
					{
						if ( a+b<c+cmp3 || b-a < cmp3+c-b )
						{
							a++;
							b++;
						}
					}
				}
			}
		}
	}

	else if (niveau==6)
	{
		if (b>a+cmp1)
		{
			b=b+vrai1;
			if (branchement1==1)
			{
				if (a<c+cmp2 && b<c+cmp2)
				{
					c=c-vrai2;
					if (branchement2==1)
					{
						if ( a+b<c+cmp3 || b-a < cmp3+c-b )
						{
							a++;
							b++;
						}
						else
						{
							a--;
							b--;
						}
					}
				}
				else
				{
					c=c+faux2;
					a++;
					if (branchement2==0)
					{
						if ( a+b<c+cmp3 || b-a < cmp3+c-b )
						{
							a++;
							b++;
						}
						else
						{
							a--;
							b--;
						}
					}
				}
			}
		}
		else
		{
			a=a+faux1;
			if (branchement1==0)
			{
				if (a<c+cmp2 && b<c+cmp2)
				{
					c=c-vrai2;
					if (branchement2==1)
					{
						if ( a+b<c+cmp3 || b-a < cmp3+c-b )
						{
							a++;
							b++;
						}
						else
						{
							a--;
							b--;
						}
					}
				}
				else
				{
					c=c+faux2;
					a++;
					if (branchement2==0)
					{
						if ( a+b<c+cmp3 || b-a < cmp3+c-b )
						{
							a++;
							b++;
						}
						else
						{
							a--;
							b--;
						}
					}
				}
			}
		}
	}

	printf("%d,%d,%d\n",a,b,c);
  return 0;

}
