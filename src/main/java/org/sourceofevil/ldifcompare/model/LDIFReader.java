package org.sourceofevil.ldifcompare.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.text.NumberFormat;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sourceofevil.ldifcompare.LDIFCompare;

public class LDIFReader implements Runnable
{
	private static final Log newerLogger = LogFactory.getLog("newerentities");
	private static final Log missingLogger = LogFactory.getLog("missingentities");
	private static final Log md5Logger = LogFactory.getLog("md5differences");	
	private static final Log logger = LogFactory.getLog("ldifreader");
	private LDAPEntities targetLDAPEntities;
	private LDAPEntities missingLDAPEntities;
	private LDAPEntities newerLDAPEntities;
	private LDAPEntities differentLDAPEntities;
	private LDIFCompare ldifcompare;
	private NumberFormat numberFormat = NumberFormat.getInstance();

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");	
	
	protected Thread t;
	
	File workingDirectory = null;
	File targetLDIF = null;

    public void start()
	{
    	logger.info("[LDIFReader] Starting...");
		
		if ( workingDirectory == null )
		{
			logger.fatal("[LDIFReader] No LDIF directory set");
			return;
		}
		if ( !workingDirectory.exists() || !workingDirectory.isDirectory() || !workingDirectory.canRead() )
        {
        	logger.fatal("[LDIFReader] Check LDIF directory '" + workingDirectory + "' exists and is readable");		
        	return;
        }
		
		t = new Thread(this);
		t.start();
	}

	public void stop()
	{
		logger.info("[LDIFReader] Stopping");
		
		t = new Thread(this);
		t.interrupt();
		
		ldifcompare.stop();
	}

	private void processFile(boolean targetFile, File file)
	{
		int lineCount = 0;
		boolean keepReading = true;

		long start = new Date().getTime();
		String line;
		String dn = null;
		StringBuilder sb = new StringBuilder();
		Entity entity = null;
		Date updated = null;
		Date d = null;
		BufferedReader reader = null;
		InputStreamReader isr = null;
		FileInputStream fis = null;
		GZIPInputStream gis = null;
		int entitiesFound = 0;
		int entitiesNewer = 0;
		int entitiesOlder = 0;
		int entitiesMissing = 0;
		int entitiesDiscarded = 0;
		int entitiesDifferent = 0;	
		int entitiesSame = 0;
		
		if ( file != null )
		{
			try
			{
				fis = new FileInputStream(file);
				
				if ( file.getName().endsWith(".gz") || file.getName().endsWith(".gzip") ) 
				{	
					gis = new GZIPInputStream(fis);
					isr = new InputStreamReader(gis, "utf-8");
				}
				else
					isr = new InputStreamReader(fis, "utf-8");

				reader = new BufferedReader(isr);
				
				logger.info("[LDIFReader] Processing '" + file + "'");
				
				keepReading = true;
				lineCount = 0;

				while(keepReading) 
				{
				    line = reader.readLine();
					
				    if (line == null) 
				    {
				    	// add final entity for EOF
				    	if ( dn != null )
						{
				    		ldifcompare.incrementTotalEntitiesProcessed();
				    		if (!dn.endsWith("rtToken=RefreshTokens,ou=Tokens,dc=reuters,dc=com") )
							{	
				    			if ( targetLDAPEntities.hasEntity(dn) && !targetLDAPEntities.getEntity(dn).getMd5().equals(DigestUtils.md5Hex(sb.toString())))
								{
									md5Logger.info("[MD5] " + targetLDAPEntities.getEntity(dn).getShortDn() + " - " + targetLDAPEntities.getEntity(dn).getShortDn() + " from '" + file + "' has a different MD5 hash than the entity found in '" + targetLDAPEntities.getEntity(dn).getFiles() + "'");	    						
									entitiesDifferent++;
								}
				    			if ( !targetLDAPEntities.hasEntity(dn) )
								{
									entity = new Entity(dn,updated,file,DigestUtils.md5Hex(sb.toString()));
									if (!targetFile)
									{
										entity.setContents(sb.toString());
										logger.info("[LDIFReader] MISSING - " + entity.toString());
										missingLogger.info(entity.toString());
										missingLDAPEntities.addEntity(entity);
										entitiesMissing++;
									}
									else
									{
										logger.debug("[LDIFReader] NEW     - " + entity.toString());
										targetLDAPEntities.addEntity(entity);
									}
								}
								else if ( updated != null && targetLDAPEntities.isEntityNewer(dn, updated) )
								{
									d = targetLDAPEntities.getEntity(dn).getTimestamp();
									entity = targetLDAPEntities.updateEntity(dn,updated,file);
									entity.setContents(sb.toString());
									logger.info("[LDIFReader] NEWER   - " + entity.toString() + " vs " + d);
									newerLogger.info(entity.toString());
									newerLDAPEntities.addEntity(entity);
									entitiesNewer++;
								}
								else if ( updated != null && targetLDAPEntities.isEntitySame(dn, updated) )
								{
									entity = targetLDAPEntities.updateEntity(dn,file);
									logger.debug("[LDIFReader] SAME    - " + entity.toString());
									entitiesSame++;
								}
								else if ( updated != null && targetLDAPEntities.isEntityOlder(dn, updated) )
								{
									logger.debug("[LDIFReader] OLDER   - " + entity.toString() + " vs " + updated);
									entitiesOlder++;
								}
							}
							else
								entitiesDiscarded++;
				    		sb.setLength(0);
				    		updated = null;
							entity = null;
						}
				    	long now = System.currentTimeMillis() - start;
				    	logger.info("[LDIFReader] Finished processing '" + file + "' in " + DurationFormatUtils.formatDurationWords(now, true, true) + " having processed " + numberFormat.format(lineCount) + " lines finding " + numberFormat.format(entitiesFound) + " total entities but discarding " + numberFormat.format(entitiesDiscarded) + " refresh tokens");
				    	
				    	if (!targetFile)
				    	{
				    		logger.info("[LDIFReader] Brand New Entities Found: " + numberFormat.format(entitiesMissing) + " - Newer Entities Found: " + numberFormat.format(entitiesNewer) + " - Older Entities Found: " + numberFormat.format(entitiesOlder) + " - Entities with Differences Found: " + numberFormat.format(entitiesDifferent) + " - Identical Entities Found: " + numberFormat.format(entitiesSame));
				    	}
				    	break;
                    }
				    else 
				    {
                        lineCount++;

                        sb.append(line);
                        
						if ( line.length() != 0 && line.startsWith("dn: ") )
						{
							entitiesFound++;
				    		ldifcompare.incrementTotalEntitiesProcessed();
							if ( dn != null )
							{
								if (!dn.endsWith("rtToken=RefreshTokens,ou=Tokens,dc=reuters,dc=com") )
								{	
									if ( targetLDAPEntities.hasEntity(dn) && !targetLDAPEntities.getEntity(dn).getMd5().equals(DigestUtils.md5Hex(sb.toString())) )
									{
										entity = new Entity(dn,updated,file,DigestUtils.md5Hex(sb.toString()));
										md5Logger.info("[MD5] " + targetLDAPEntities.getEntity(dn).getShortDn() + " from " + file + " has a different MD5 hash than the entity found in '" + targetLDAPEntities.getEntity(dn).getFiles() + "'");	    						
										entitiesDifferent++;
										differentLDAPEntities.addEntity(entity);
									}
					    			if ( !targetLDAPEntities.hasEntity(dn) )
									{
										entity = new Entity(dn,updated,file,DigestUtils.md5Hex(sb.toString()));
										if (!targetFile)
										{
											entity.setContents(sb.toString());
											logger.info("[LDIFReader] MISSING - " + entity.toString());
											missingLogger.info(entity.toString());
											missingLDAPEntities.addEntity(entity);
											entitiesMissing++;
										}
										else
										{
											logger.debug("[LDIFReader] NEW     - " + entity.toString());
											targetLDAPEntities.addEntity(entity);
										}
									}
									else if ( updated != null && targetLDAPEntities.isEntityNewer(dn, updated) )
									{
										d = targetLDAPEntities.getEntity(dn).getTimestamp();
										entity = targetLDAPEntities.updateEntity(dn,updated,file);
										entity.setContents(sb.toString());
										logger.info("[LDIFReader] NEWER   - " + entity.toString() + " vs " + d);
										newerLogger.info(entity.toString());
										newerLDAPEntities.addEntity(entity);
										entitiesNewer++;
									}
									else if ( updated != null && targetLDAPEntities.isEntitySame(dn, updated) )
									{
										entity = targetLDAPEntities.updateEntity(dn,file);
										logger.debug("[LDIFReader] SAME    - " + entity.toString());
										entitiesSame++;
									}
									else if ( updated != null && targetLDAPEntities.isEntityOlder(dn, updated) )
									{
										entity = targetLDAPEntities.getEntity(dn);
										logger.debug("[LDIFReader] OLDER   - " + entity.toString() + " vs " + updated);
										entitiesOlder++;
									}
								}
								else
									entitiesDiscarded++;
								sb.setLength(0);
								updated = null;
							}
							dn = line;
						}
						else if ( line.startsWith("createTimestamp: ") )
							updated = parseDate(dn, line, "createTimestamp: ", false, updated);
						else if ( line.startsWith("modifyTimestamp: ") )
							updated = parseDate(dn, line, "modifyTimestamp: ", false, updated);
						else if ( line.startsWith("rtLastSuccessLogin: ") )
							updated = parseDate(dn, line, "rtLastSuccessLogin: ", false, updated);
						else if ( line.startsWith("pwdChangedTime: ") )
							updated = parseDate(dn, line, "pwdChangedTime: ", true, updated);
						else if ( line.startsWith("rtFTLCompleteFlagSetTime: ") )
							updated = parseDate(dn, line, "rtFTLCompleteFlagSetTime: ", true, updated);
						else if ( line.startsWith("rtProfileLastUpdatedByAAA: ") )
							updated = parseDate(dn, line, "rtProfileLastUpdatedByAAA: ", true, updated);
						else if ( line.startsWith("rtLegalConsentFlagSetTime: ") )
							updated = parseDate(dn, line, "rtLegalConsentFlagSetTime: ", true, updated);
				    }
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				if ( reader != null )
				{
					try
					{
						reader.close();
					}
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
				if ( isr != null )
				{
					try
					{
						isr.close();
					}
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
				if ( gis != null )
				{
					try
					{
						gis.close();
					}
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
				if ( fis != null )
				{
					try
					{
						fis.close();
					}
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public Date parseDate(String dn, String line, String fieldname, boolean strip, Date updated)
	{
		Date d;

		line = line.substring(fieldname.length());
		if ( strip )
			line = line.substring(0, line.indexOf(".")-1);
		
		try
		{
			d = sdf.parse(line);
			if (updated == null)
			{
				logger.trace("new " + fieldname + dn + " - " + d);
				updated = d;
			}
			else if (updated.before(d))
			{
				logger.trace("newer " + fieldname + dn + " - " + d);
				updated = d;
			}
		}
		catch (ParseException|NumberFormatException e) 
		{
			logger.warn("Could not parse '" + fieldname + "' from: '" + line + "' on " + dn);
		}

		return updated;
	}

	public void run()
	{		
        if ( targetLDIF.exists() && targetLDIF.canRead() )
        {
            processFile(true,targetLDIF);
        }      
        
		File[] files;
		files = listFiles();
				
		if ( files != null && files.length > 0 )
		{
			for (int i=0; i < files.length; i++)
			{
				if ( !files[i].equals(targetLDIF) )
					processFile(false,files[i]);
			}
		}
		this.stop();
	}

	private File[] listFiles()
	{
		FilenameFilter filenameFilter = (dir, name) -> ( name.endsWith(".ldif") || name.endsWith(".ldif.gz") || name.endsWith(".ldif.gzip") );
		
		File[] files = workingDirectory.listFiles(filenameFilter);		
		if ( files != null )
		{
			Arrays.sort(files, (f1, f2) -> String.valueOf(f2.getName()).compareTo(f1.getName()));	
		}
		
		return files;
	}

	public LDIFReader(LDIFCompare ldifcompare, LDAPEntities ldapentities, LDAPEntities missingLDAPEntities, LDAPEntities newerLDAPEntities, LDAPEntities differentLDAPEntities, File workingDirectory, File targetLDIF) 
	{
		super();
		this.ldifcompare = ldifcompare;
		this.targetLDAPEntities = ldapentities;
		this.missingLDAPEntities = missingLDAPEntities;
		this.newerLDAPEntities = newerLDAPEntities;
		this.differentLDAPEntities = differentLDAPEntities;
		this.workingDirectory = workingDirectory;
		this.targetLDIF = targetLDIF;
	}
}
