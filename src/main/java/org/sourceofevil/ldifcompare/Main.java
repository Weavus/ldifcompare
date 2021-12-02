package org.sourceofevil.ldifcompare;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main
{	
	private static final Log logger = LogFactory.getLog("ldifcompare");	
	
	private static File getLDIFFolder(String[] args)
	{
		File ldifFolder = null;

		if ( args.length > 0 )
		{
			if ( args.length > 0 )
			{
				ldifFolder = new File(args[0]);
                if ( !ldifFolder.exists() || !ldifFolder.isDirectory() || !ldifFolder.canRead() )
                {
                	logger.info("Check LDIF directory '" + args[0] + "' exists and is readable");		
                	System.exit(1);
                }
			}
		}
		else
		{
			logger.info("Usage: ldifcompare <ldif folder> <(optional) target ldif file>");		
			System.exit(1);
		}
		return ldifFolder;
	}

	private static File getTargetFile(String[] args)
	{
		File targetFile = null;

		if ( args.length > 1 )
		{
			targetFile = new File(args[1]);
			if ( !targetFile.exists() || !targetFile.isFile() || !targetFile.canRead() )
			{
				logger.info("Check Target LDIF file '" + args[1] + "' exists and is readable");		
				System.exit(1);
			}
		}
		return targetFile;
	}

	public static void main(String[] args) 
	{
		File ldifFolder = getLDIFFolder(args);
		File targetFile = getTargetFile(args);

		boolean done = false;
		logger.info("Starting LDIFCompare using LDIF folder " + ldifFolder);		
		if (targetFile != null)
			logger.info("Using " + targetFile + " as the target LDIF");	
		
		String[] serviceResources = {"ldifcompare.xml"};
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(serviceResources);					
				
		LDIFCompare ldifcompare = (LDIFCompare) ctx.getBean("LDIFCompare");		
		ldifcompare.setWorkingDirectory(ldifFolder);
		ldifcompare.setTargetLDIF(targetFile);
		ldifcompare.start();
		
		while (!done)
		{
			try
			{					
				Thread.sleep(60000);
				ldifcompare.outputStatus();
			}
			catch (InterruptedException e) 
			{
				done = true;
				Thread.currentThread().interrupt();
			}
		}

		ldifcompare.stop();		
		ctx.close();
		logger.info("Exiting LDIFCompare");	
	}
}