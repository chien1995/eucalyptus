/**
 * Created at 2:04:27 PM Aug 28, 2009 by Tryggvi Larusson
 *
 * Copyright (C) 2009 Tryggvi Larusson All Rights Reserved.
 */
package edu.ucsb.eucalyptus.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion.User;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.UsageCounter;
import edu.ucsb.eucalyptus.cloud.entities.UsageType;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;

/**
 * <p>
 * TODO tryggvil Describe Type UsageManagement
 * </p>
 * 
 * @author <a href="mailto:tryggvi.larusson[at]gmail.com">tryggvil</a>
 */
public class UsageManagement {

    private static Logger LOG = Logger.getLogger( UsageManagement.class );

    
    public static double countHoursBetween(Date startDate, Date endDate){
    	double ret=0;
    	Calendar startCalendar = new GregorianCalendar();
    	startCalendar.setTime(startDate);
    	Calendar endCalendar = new GregorianCalendar();
    	endCalendar.setTime(endDate);
    	return countHoursBetween(startCalendar, endCalendar);
    }
	
    public static double countHoursBetween(Calendar startDate, Calendar endDate){
    	long startDateMillis = startDate.getTimeInMillis();
    	long endDateMillis = endDate.getTimeInMillis();
    	long millis = endDateMillis-startDateMillis;
    	double hours = millis/1000f/60f/60f;
    	return hours;
    }
    
	/**
	 * <p>
	 * TODO tryggvil describe method registerInstanceStartUsage
	 * </p>
	 * @param userName
	 * @param instanceId
	 * @param vmType
	 * @param creationTime
	 */
	public static void registerInstanceStartUsage(String userName,
			String instanceId, String imageId, String vmType, Date creationTime) {
		LOG.info("Registering instance start of use for: userName:"+userName+", instanceId:"+instanceId+", imageId:"+imageId+", vmType:"+vmType+", creationTime:"+creationTime);
	    EntityWrapper<UsageCounter> db = new EntityWrapper<UsageCounter>();

		try {
			
			UsageCounter counter = new UsageCounter();
			counter.setStartTime(creationTime);
			counter.setUsageInstanceKey(instanceId);
			String operationName=UsageType.OPERATION_RUNINSTANCES;
			String usageType=UsageType.USAGE_TYPE_BOX_USAGE;
			String usageSubType=vmType;
			UsageType uType;
			uType = UsageType.findBy(operationName, usageType, usageSubType);
			counter.setUsageType(uType);
			UserInfo user = UserInfo.named(userName);
			counter.setUser(user);
			
			db.add(counter );
			db.commit();
			
		} catch (EucalyptusCloudException e) {
		    db.rollback();
			throw new RuntimeException(e);
		}
	
	}
	
	public static void registerInstanceStopUsage(String userName,
			String instanceId, Date stop) {
		LOG.info("Registering instance stop of use for: userName:"+userName+", instanceId:"+instanceId+", stop:"+stop);
		
		 EntityWrapper<UsageCounter> db = new EntityWrapper<UsageCounter>();

		 try {
			UsageCounter counter = findCounterBy(userName,instanceId);
			
			Date start = counter.getStartTime();
			double hoursConsumed = countHoursBetween(start,stop);			
			counter.setAmount(hoursConsumed);
			db.merge(counter);
			db.commit();
		} catch (Exception e) {
		    db.rollback();
			throw new RuntimeException(e);
		}
		
	}

	/**
	 * <p>
	 * TODO tryggvil describe method findCounterBy
	 * </p>
	 * @param userName
	 * @param instanceId
	 * @return
	 */
	private static UsageCounter findCounterBy(String userName, String instanceId) {
		try {
			UserInfo user = UserInfo.named(userName);
			return UsageCounter.findBy(user, instanceId);
		} catch (EucalyptusCloudException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) throws ParseException{
		
	     SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy");

		 String sDateFrom = "27-August-09";
	     Date firstDay = (Date)formatter.parse(sDateFrom); 
         Calendar cal=Calendar.getInstance();
         String sDateTo = "31-August-09";
	     //Date lastDay = (Date)formatter.parse(sDateTo); 
		 Date lastDay = new Date();
		//alendar firstDay = new GregorianCalendar(2006, Calendar.JULY, 15);
		//firstDay.add(Calendar.MILLISECOND, 1000*60*30);
		//Calendar lastDay = new GregorianCalendar(2006, Calendar.JULY, 17);
		double hours = countHoursBetween(firstDay, lastDay);
		System.out.println("There are "+hours+" hours between "+firstDay+" and "+lastDay);
		
	}

}
