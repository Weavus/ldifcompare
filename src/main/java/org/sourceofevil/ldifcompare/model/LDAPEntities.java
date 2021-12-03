package org.sourceofevil.ldifcompare.model;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

public class LDAPEntities
{		
    private final HashMap<String, Entity> entities;

    public void addEntity(Entity entity)
    {
    	this.entities.put(entity.getDn(), entity);
    }
    
    public Entity getEntity(String dn)
    {
    	return this.entities.get(dn);
    }
    
    public boolean hasEntity(String dn)
    {
    	return this.entities.containsKey(dn);
    }
    
    public Entity updateEntity(String dn, Date date, File file)
    {
    	if ( this.entities.containsKey(dn) )
    	{
    		entities.get(dn).update(date, file);
    		return entities.get(dn);
    	}
    	return null;
    }
    
    public Entity updateEntity(String dn, File file)
    {
    	if ( this.entities.containsKey(dn) )
    	{
    		entities.get(dn).update(file);
    		return entities.get(dn);
    	}
    	return null;
    }
    
    public boolean isEntityNewer(String dn, Date date)
    {
    	return ( this.entities.containsKey(dn) && ( entities.get(dn).getTimestamp() == null || entities.get(dn).getTimestamp().before(date) ) ); 
    }
    
	public boolean isEntityOlder(String dn, Date date)
    {
    	return ( this.entities.containsKey(dn) && ( entities.get(dn).getTimestamp() == null || entities.get(dn).getTimestamp().after(date) ) );
    }

    public boolean isEntitySame(String dn, Date date)
    {
    	return ( this.entities.containsKey(dn) && entities.get(dn).getTimestamp() != null && entities.get(dn).getTimestamp().equals(date) ); 
    }
    
	public LDAPEntities() 
	{
		super();
		this.entities = new HashMap<>();
	}
	
	public int getSize()
	{
		return entities.size();
	}
}
