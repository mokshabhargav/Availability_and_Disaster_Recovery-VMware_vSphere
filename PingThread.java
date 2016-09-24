

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Alarm;
import com.vmware.vim25.mo.AlarmManager;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class PingThread extends Thread {
	ServiceInstance si = null;
	int duration;
	
	PingThread(ServiceInstance si) 
	{
		this.si = si;
	}
	//new constructor
	PingThread(ServiceInstance si, int period) 
	{
		this.si = si;
		this.duration = period;
	}

	public void run() 
	{
		HostSystem newHost;
		String newHostIp;
		String s = null;
		String ip = null;
		VirtualMachine vm1 = null;
		while (true) 
		{
			try 
			{
				Folder rootFolder = si.getRootFolder();
				Datacenter dc = (Datacenter) new InventoryNavigator(rootFolder).searchManagedEntity("Datacenter", "T08-DC");
				ManagedEntity[] mesHost = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");

				for (int j = 0; j < mesHost.length; j++) 
				{
					boolean isHostActive = false;
					HostSystem hs = (HostSystem) mesHost[j];
					VirtualMachine[] mes = hs.getVms();
					
					for (int i = 0; i < mes.length; i++) 
					{
						VirtualMachine vm = (VirtualMachine) mes[i];
						System.out.println("VM name : " + vm.getName() + " IP : " + vm.getGuest().getIpAddress() );
						
						String hostip = hs.getName();
						
						//CREATE ALARM HERE FOR EACH VM
						AlarmManagerClass am = new AlarmManagerClass(si,vm.getName());
						am.setAlarm();
						
					
						
						if (vm.getSummary().runtime.powerState.toString().equals("poweredOn")
								&& (vm.getGuestHeartbeatStatus().toString().equals("green") )) // or gray || vm.getGuestHeartbeatStatus().toString().equals("gray")
						{ 

							System.out.println("into the power-on method for" + vm.getName());
							ip = vm.getGuest().getIpAddress();
							
							try 
							{
								
								if ((ip != null) && Ping.pingCommon(ip)) 
								{
									System.out.println("Ping to VM : " + vm.getName() + " Successful");
								} 
								else 
								{
									System.out.println("In ping thread ------Unable to reach VM :" +  vm.getName());
								 
									
								
									 if (hostip != null && Ping.pingCommon(hostip)) 
									{										
										/*  CHECK FOR TRIGGERED ALARM  */
										if(am.getAlarmStatus())
											System.out.println("@!@!@!@!@! ALARM TRIGGERED, user has switched off VM  : "+ vm.getName());	
										else 
										{
											System.out.println("Ping to Host : " + hs.getName()	+ " Successful"); 
											System.out.println("Started disaster recovery Thread for VM : " + vm.getName());
											new DisasterRecovery(vm).start();
										}
									} 
									else 
									{
										System.out.println("Trying to ON host....");
										
										
										
										VirtualMachine vHostVM = RecoverHost.getvHostFromAdminVCenter(hs.getName().substring(11, hs.getName().length()));
						                Task taskHost = vHostVM.revertToCurrentSnapshot_Task(null);
						                if (taskHost.getTaskInfo().getState() == taskHost.getTaskInfo().getState().success) {
						                    System.out.println("Recovery Handler: vHost has been recovered on the admin vCenter..");
						                }
						               
						                if(RecoverHost.reconnectHostandRecoverVM(vm,hs)){
						                    System.out.println("Recovery Handler: Host reconnected");
						                }
										
//										boolean res = ManageHost.revert2PreviousVHSnapshot(hs);
//										if(res == true)
//											System.out.println("Reverted the host snapshot");
										else
										{
										
										newHostIp=findNextActiveHost();
										if(newHostIp!="")
										{
											RegisterVM.registerVM_toHost(dc, rootFolder, newHostIp, hs);
											
//											newHost = (HostSystem) new InventoryNavigator(rootFolder).searchManagedEntity("HostSystem",newHostIp);
//											ComputeResource cr = (ComputeResource) newHost.getParent();
//
//											Task task = vm.migrateVM_Task(cr.getResourcePool(),newHost,	VirtualMachineMovePriority.highPriority,
//													VirtualMachinePowerState.poweredOff);
//											System.out.println("in migrating if loop");
//											if(task.waitForTask() == task.SUCCESS)
//											{
//												System.out.println("Migration of VM to new host is complete.");				
//											}
											
//										//RegisterVM.registerVM_toHost(rootFolder,vm);
//										System.out.println("Next active host is: " +hostip);			
//										System.out.println("Vm to be moved is: "+vm.getName());
//										System.out.println("Starting uploading the VMDK file to create new VM..");
//										ImportLocalOvfVApp.importOvf("C:\\Users\\Maithili\\Desktop\\TEAM14_Ubuntu07","root", "12!@qwQW","130.65.132.201", "C:\\Users\\Maithili\\Desktop\\TEAM14_Ubuntu07","130.65.133.210"); 
//										System.out.println("Completed uploading the VMDK file!");
										
										}
										else
										{
											System.out.println("Active hosts not found");
											if(ManageHost.addHost("130.65.132.203"))
											{
												RegisterVM.registerVM_toHost(dc, rootFolder, "130.65.132.203", hs);
//												System.out.println("Next active host is: " +hostip);			
//												System.out.println("Vm to be moved is: "+vm.getName());
//												System.out.println("Starting uploading the VMDK file to create new VM..");
//												ImportLocalOvfVApp.importOvf("C:\\Users\\Maithili\\Desktop\\TEAM14_Ubuntu07","root", "12!@qwQW","130.65.132.201", "C:\\Users\\Maithili\\Desktop\\TEAM14_Ubuntu07","130.65.133.210"); 
//												System.out.println("Completed uploading the VMDK file!");
//												System.out.println("Host 132.203 added");
												
											}
											
											
											else
												System.out.println("Error while adding vhost to vcenter");
												
										}
										
										//}//if(res== false)
										
										//boolean alarmStat = am.getHostAlarmStatus(hs);
										//System.out.println(alarmStat);
										
										/*for(int k=0; k<mesHost.length; k++)
										{
											
											HostSystem hs_new = (HostSystem) mesHost[k];
											if (Ping.pingCommon(hs_new.getName()) == true);
											System.out.println("Vm in host " + vm.getName());
											
										} */
										
										
										
										/*
										 *  THIS MAY CAUSE A PROBLEM
										 *  WHEN HOST IS NOT RECOVERED YET, AND IF THE LOOP FOR HOST STARTS
										 *  THEN AGAIN IT WILL CALL HOST FOR RECOVERY
										 *  SO SOME SORT OF COMMUNICATION IS NEEDED BETWEEN THE HOST RECOVERY AND PING
										 * /
										if(isHostActive == false)
										{
											isHostActive = true;
											System.out.println("Could not Ping Host " + hs.getName() + " and it is being checked for Power off/NIC disabled"); 
											new HostStatus(hs.getName()).start();//to check HOST conditions
											
										}
									} */
										
										}	
								}//else
								}//else
							}//try
							catch (Exception e) 
							{
								System.out.println("Exception occured while Pinging : " + e.getMessage());
								System.out.println(e.getStackTrace());
							}
						}//if
						
						else
							System.out.println(vm.getName() + " is in powered off state");
						
						
						
						am.removeAlarm(vm);
						
					}//for
				}//for
				
					
			System.out.println("Sleep Ping thread for " + duration + " minutes");
			System.out.println();
			Thread.currentThread().sleep(duration * 60 * 1000);
/*			//TEST
			for (int j = 0; j < mesHost.length; j++) 
			{
				
				HostSystem hs = (HostSystem) mesHost[j];
				VirtualMachine[] mes = hs.getVms();
				
				for (int i = 0; i < mes.length; i++) 
				{
					VirtualMachine vm = (VirtualMachine) mes[i];
					alarmManager am = new alarmManager(si,vm.getName());
					if(am.check_Alarm())
					{
						System.out.println("@!@!@!@!@! ALARM TRIGGERED, user has switched off VM : "+ vm.getName());
					}
					am.remove_alarm(vm);
				} 
			} */
			//TEST
			}//try 
			catch (Exception e) 
			{
				System.out.println("Exception occured while getting Session instance : " + e.getStackTrace());
				System.out.println(e.getStackTrace());

			}
			
			/*
			 *I can move this try catch and include it in the above catch 
			 *
			System.out.println("Sleep Ping thread for 120 seconds");
			try 
			{
				Thread.currentThread().sleep(120 * 1000);// sleep for 120 seconds
			/*
			 * i can remove alarms for all VM's HERE2
			 *
			} 
			catch (Exception e) 
			{
				System.out.println("Ping Thread sleep interrupted" + e.getMessage());
				System.out.println(e.getStackTrace());
			}
			*/

		}//while

	}//run

	/*public boolean doCommand(String cmd, String ip) throws Exception 
	{
		String s = null;
		int count = 0;
		ProcessBuilder pb = new ProcessBuilder("ping", ip);
		Process process = pb.start();
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		if(ip == null)
			System.out.println("Caution : Trying to Ping with IP as null");
		
		while ((s = stdInput.readLine()) != null) 
		{
			if (s.equals("Request timed out.")) 
			{
				if(count == 2)
					return false;
				count++;
			}
		}
		
		if ((s = stdError.readLine()) != null) 
		{
			return false;
		}
		return true;

	} */
	
	
	public String findNextActiveHost()
	{
		String hostip="";
		HostSystem hs;
		try
		{
			Folder rootFolder = si.getRootFolder();
			ManagedEntity[] mesHost = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
			for(int i=0;i<mesHost.length;i++)
			{
				hs = (HostSystem) mesHost[i];
				hostip=hs.getName();
				if(hostip!=null && Ping.pingCommon(hostip))
					return hostip;
				else
					hostip="";
			}
		}catch(Exception e)
		{
			System.out.println("Exception " +e);
		}

		return hostip;
	}

}

