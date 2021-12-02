package org.sourceofevil.ldifcompare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.text.NumberFormat;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sourceofevil.ldifcompare.model.LDAPEntities;
import org.sourceofevil.ldifcompare.model.LDIFReader;

@SpringBootApplication
public class LDIFCompare {

	private static final Log logger = LogFactory.getLog("ldifcompare");
	
	private final LDAPEntities ldapentities = new LDAPEntities();
	private final LDAPEntities missingLDAPEntities = new LDAPEntities();
	private final LDAPEntities newerLDAPEntities = new LDAPEntities();
	private final LDAPEntities differentLDAPEntities = new LDAPEntities();
	
    private long totalEntitiesProcessed = 0;

	private long start = System.currentTimeMillis();
	File workingDirectory = null;
	File targetLDIF = null;

	public void outputStatus() 
	{
		NumberFormat numberFormat = NumberFormat.getInstance();

		long now = System.currentTimeMillis() - start;
		long eps = totalEntitiesProcessed;
		if ( totalEntitiesProcessed > 0 && now > 0 && ( now / 1000 ) > 0)
			eps = totalEntitiesProcessed / ( now / 1000 );
		logger.info("[LDIFCompare] Running: " + DurationFormatUtils.formatDurationWords(now, true, true) + " - Processed: " + numberFormat.format(totalEntitiesProcessed) + " - Unique Entities Found: " + numberFormat.format(ldapentities.getSize()) + " - Entities Per Second: " + numberFormat.format(eps));
		logger.info("[LDIFCompare] Total Missing Entities Found: " + numberFormat.format(missingLDAPEntities.getSize()) + " - Total Newer Entities Found: " + numberFormat.format(newerLDAPEntities.getSize()) + " - Total Different Entities Found: " + numberFormat.format(differentLDAPEntities.getSize()));
	}
	
	public void start()
	{
		logger.info("[LDIFCompare] Starting...");
		LDIFReader ldifreader = new LDIFReader(this,ldapentities,missingLDAPEntities,newerLDAPEntities,differentLDAPEntities,workingDirectory,targetLDIF);
		ldifreader.start();
	}
	
	public void stop()
	{
		outputStatus();
		logger.info("[LDIFCompare] Stopping");
		System.exit(0);
	}
	
	public final void setWorkingDirectory(File workingDirectory) 
	{
		this.workingDirectory = workingDirectory;
	}

	public final void setTargetLDIF(File targetLDIF) 
	{
		this.targetLDIF = targetLDIF;
	}

	public final void incrementTotalEntitiesProcessed() 
	{
		this.totalEntitiesProcessed++;
	}

	public static void main(String[] args) {
		SpringApplication.run(LDIFCompare.class, args);
	}
}
