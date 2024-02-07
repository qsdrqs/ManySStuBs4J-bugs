/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.config;

import java.util.ArrayList;
import java.util.List;

import org.openhab.binding.zwave.internal.protocol.ConfigurationParameter;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEventListener;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass.CommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveConfigurationCommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveAssociationEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeSerializer;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Z Wave configuration class Interfaces between the REST services using the
 * OpenHABConfigurationService interface. It uses the ZWave product database to
 * configure zwave devices.
 * 
 * @author Chris Jackson
 * @since 1.4.0
 * 
 */
public class ZWaveConfiguration implements OpenHABConfigurationService, ZWaveEventListener {
	private static final Logger logger = LoggerFactory.getLogger(ZWaveConfiguration.class);

	private ZWaveController zController = null;

	public ZWaveConfiguration() {
	}

	public ZWaveConfiguration(ZWaveController controller) {
		this.zController = controller;

		// Register the service
		FrameworkUtil.getBundle(getClass()).getBundleContext()
				.registerService(OpenHABConfigurationService.class.getName(), this, null);
	}

	@Override
	public String getBundleName() {
		return "zwave";
	}

	@Override
	public String getVersion() {
		return FrameworkUtil.getBundle(getClass()).getBundleContext().getBundle().getVersion().toString();
	}

	@Override
	public List<OpenHABConfigurationRecord> getConfiguration(String domain) {
		// We only deal with top level domains here!
		if (domain.endsWith("/") == false)
			return null;

		List<OpenHABConfigurationRecord> records = new ArrayList<OpenHABConfigurationRecord>();
		OpenHABConfigurationRecord record;
		ZWaveNode node;

		if (domain.equals("status/")) {
			// Return the z-wave status information

			return null;
		}
		if (domain.startsWith("products/")) {
			ZWaveProductDatabase database = new ZWaveProductDatabase();

			String[] splitDomain = domain.split("/");

			switch (splitDomain.length) {
			case 1:
				// Just list the manufacturers
				for (ZWaveDbManufacturer manufacturer : database.GetManufacturers()) {
					record = new OpenHABConfigurationRecord(domain + manufacturer.Id.toString() + "/", manufacturer.Name);

					records.add(record);
				}
				break;
			case 2:
				// Get products list
				if (database.FindManufacturer(Integer.parseInt(splitDomain[1])) == false)
					break;

				for (ZWaveDbProduct product : database.GetProducts()) {
					record = new OpenHABConfigurationRecord(domain + product.Reference.get(0).Type + "/" + product.Reference.get(0).Id + "/", product.Model);
					record.value = database.getLabel(product.Label);
					records.add(record);
				}
				break;
			case 4:
				// Get product
				if (database.FindProduct(Integer.parseInt(splitDomain[1]), Integer.parseInt(splitDomain[2]), Integer.parseInt(splitDomain[3])) == false)
					break;

				record = new OpenHABConfigurationRecord(domain + "parameters/", "Configuration Parameters");
				records.add(record);

				record = new OpenHABConfigurationRecord(domain + "associations/", "Association Groups");
				records.add(record);
				break;
			case 5:
				// Get product
				if (database.FindProduct(Integer.parseInt(splitDomain[1]), Integer.parseInt(splitDomain[2]), Integer.parseInt(splitDomain[3])) == false)
					break;

				if (splitDomain[4].equals("parameters")) {
					List<ZWaveDbConfigurationParameter> configList = database.getProductConfigParameters();
					// Loop through the parameters and add to the records...
					for (ZWaveDbConfigurationParameter parameter : configList) {
						record = new OpenHABConfigurationRecord(domain, "configuration" + parameter.Index,
								database.getLabel(parameter.Label), true);

						if (parameter != null)
							record.value = parameter.Default;

						// Add the data type
						if (parameter.Type.equalsIgnoreCase("list")) {
							record.type = OpenHABConfigurationRecord.TYPE.LIST;

							for (ZWaveDbConfigurationListItem item : parameter.Item)
								record.addValue(Integer.toString(item.Value), database.getLabel(item.Label));
						} else if (parameter.Type.equalsIgnoreCase("byte"))
							record.type = OpenHABConfigurationRecord.TYPE.BYTE;
						else
							record.type = OpenHABConfigurationRecord.TYPE.SHORT;

						// Add the description
						record.description = database.getLabel(parameter.Help);

						records.add(record);
					}
				}
				if (splitDomain[4].equals("associations")) {
					List<ZWaveDbAssociationGroup> groupList = database.getProductAssociationGroups();

					if (groupList != null) {
						// Loop through the associations and add to the
						// records...
						for (ZWaveDbAssociationGroup group : groupList) {
							record = new OpenHABConfigurationRecord(domain, "association" + group.Index + "/",
									database.getLabel(group.Label), true);

							// Add the description
							record.description = database.getLabel(group.Help);

							records.add(record);
						}
					}
				}
				break;
			}
			return records;
		}

		// All domains after here must have an initialised ZWave network
		if (zController == null)
			return null;

		if (domain.equals("nodes/")) {
			ZWaveProductDatabase database = new ZWaveProductDatabase();
			// Return the list of nodes
			for (int nodeId = 0; nodeId < 256; nodeId++) {
				node = zController.getNode(nodeId);
				if (node == null)
					continue;

				if (node.getManufacturer() == 0)
					continue;

				if (node.getName() == null || node.getName().isEmpty())
					record = new OpenHABConfigurationRecord("nodes/" + "node" + nodeId + "/", "Node " + nodeId);
				else
					record = new OpenHABConfigurationRecord("nodes/" + "node" + nodeId + "/", node.getName());

				// If we can't find the product, then try and find just the
				// manufacturer
				if (database.FindProduct(node.getManufacturer(), node.getDeviceType(), node.getDeviceId()) == false) {
					if (database.FindManufacturer(node.getManufacturer()) == false) {
						record.value = "Manufacturer:" + node.getManufacturer() + " [ID:"
								+ Integer.toHexString(node.getDeviceId()) + ",Type:"
								+ Integer.toHexString(node.getDeviceType()) + "]";
					} else {
						record.value = database.getManufacturerName() + " [ID:"
								+ Integer.toHexString(node.getDeviceId()) + ",Type:"
								+ Integer.toHexString(node.getDeviceType()) + "]";
					}
				} else {
					if (node.getLocation() == null || node.getLocation().isEmpty())
						record.value = database.getProductName();
					else
						record.value = database.getProductName() + ": " + node.getLocation();
				}

				// Add the save button
				record.addAction("Save", "Save Node");
				records.add(record);
			}
			return records;
		}
		if (domain.startsWith("nodes/node")) {
			String nodeNumber = domain.substring(10);
			int next = nodeNumber.indexOf('/');
			String arg = null;
			if (next != -1) {
				arg = nodeNumber.substring(next + 1);
				nodeNumber = nodeNumber.substring(0, next);
			}
			int nodeId = Integer.parseInt(nodeNumber);

			// Return the detailed configuration for this node
			node = zController.getNode(nodeId);
			if (node == null)
				return null;

			ZWaveConfigurationCommandClass configurationCommandClass = (ZWaveConfigurationCommandClass) node
					.getCommandClass(CommandClass.CONFIGURATION);

			if (configurationCommandClass == null)
				return null;

			ZWaveProductDatabase database = new ZWaveProductDatabase();

			// Process the request
			if (arg.equals("")) {
				record = new OpenHABConfigurationRecord(domain, "Name", "Name", false);
				record.value = node.getName();
				records.add(record);

				record = new OpenHABConfigurationRecord(domain, "Location", "Location", false);
				record.value = node.getLocation();
				records.add(record);

				if (database.FindManufacturer(node.getManufacturer()) == false) {
					record = new OpenHABConfigurationRecord(domain, "ManufacturerID", "Manufacturer ID", true);
					record.value = Integer.toString(node.getManufacturer());
					records.add(record);
				} else {
					record = new OpenHABConfigurationRecord(domain, "Manufacturer", "Manufacturer", true);
					record.value = database.getManufacturerName();
					records.add(record);
				}

				if (database.FindProduct(node.getManufacturer(), node.getDeviceType(), node.getDeviceId()) == false) {
					record = new OpenHABConfigurationRecord(domain, "DeviceId", "Device ID", true);
					record.value = Integer.toString(node.getDeviceId());
					records.add(record);

					record = new OpenHABConfigurationRecord(domain, "DeviceType", "Device Type", true);
					record.value = Integer.toString(node.getDeviceType());
					records.add(record);
				} else {
					record = new OpenHABConfigurationRecord(domain, "Product", "Product", true);
					record.value = database.getProductName();
					records.add(record);

					record = new OpenHABConfigurationRecord(domain + "parameters/", "Configuration Parameters");
					record.addAction("Refresh", "Refresh");
					records.add(record);

					record = new OpenHABConfigurationRecord(domain + "associations/", "Association Groups");
					record.addAction("Refresh", "Refresh");
					records.add(record);
				}
			} else if (arg.equals("parameters/")) {
				if (database.FindProduct(node.getManufacturer(), node.getDeviceType(), node.getDeviceId()) != false) {
					List<ZWaveDbConfigurationParameter> configList = database.getProductConfigParameters();

					// Loop through the parameters and add to the records...
					for (ZWaveDbConfigurationParameter parameter : configList) {
						record = new OpenHABConfigurationRecord(domain, "configuration" + parameter.Index,
								database.getLabel(parameter.Label), false);

						ConfigurationParameter configurationParameter = configurationCommandClass
								.getParameter(parameter.Index);

						// Only provide a value if it's stored in the node
						// This is the only way we can be sure of its real value
						if (parameter != null)
							record.value = Integer.toString(configurationParameter.getValue());

						// Add the data type
						if (parameter.Type.equalsIgnoreCase("list")) {
							record.type = OpenHABConfigurationRecord.TYPE.LIST;

							for (ZWaveDbConfigurationListItem item : parameter.Item)
								record.addValue(Integer.toString(item.Value), database.getLabel(item.Label));
						} else if (parameter.Type.equalsIgnoreCase("byte"))
							record.type = OpenHABConfigurationRecord.TYPE.BYTE;
						else
							record.type = OpenHABConfigurationRecord.TYPE.SHORT;

						// Add the description
						record.description = database.getLabel(parameter.Help);

						records.add(record);
					}
				}
			} else if (arg.equals("associations/")) {
				if (database.FindProduct(node.getManufacturer(), node.getDeviceType(), node.getDeviceId()) != false) {
					List<ZWaveDbAssociationGroup> groupList = database.getProductAssociationGroups();

					if (groupList != null) {
						// Loop through the associations and add to the
						// records...
						for (ZWaveDbAssociationGroup group : groupList) {
							// Controller reporting associations are set to read
							// only
							record = new OpenHABConfigurationRecord(domain, "association" + group.Index + "/",
									database.getLabel(group.Label), group.SetToController);

							// Add the description
							record.description = database.getLabel(group.Help);

							// Add the action for refresh
							record.addAction("Refresh", "Refresh");

							records.add(record);
						}
					}
				}
			} else if (arg.startsWith("associations/association")) {
				if (database.FindProduct(node.getManufacturer(), node.getDeviceType(), node.getDeviceId()) != false) {

					String groupString = arg.substring(24);
					int nextDelimiter = groupString.indexOf('/');
					// String arg = null;
					if (nextDelimiter != -1) {
						// arg = nodeNumber.substring(nextDelimiter + 1);
						groupString = groupString.substring(0, nextDelimiter);
					}
					int groupId = Integer.parseInt(groupString);

					// Get the requested group so we have access to the
					// attributes
					List<ZWaveDbAssociationGroup> groupList = database.getProductAssociationGroups();
					if (groupList == null)
						return null;
					ZWaveDbAssociationGroup group = null;
					for (int cnt = 0; cnt < groupList.size(); cnt++) {
						if (groupList.get(cnt).Index == groupId) {
							group = groupList.get(cnt);
							break;
						}
					}

					if (group == null)
						return null;

					// Get the group members
					List<Integer> members = node.configAssociationGetMembers(groupId);

					for (int id = 0; id < 256; id++) {
						node = zController.getNode(id);
						if (node == null)
							continue;

						if (node.getName() == null || node.getName().isEmpty())
							record = new OpenHABConfigurationRecord(domain, "node" + id, "Node " + id, false);
						else
							record = new OpenHABConfigurationRecord(domain, "node" + id, node.getName(), false);

						record.type = OpenHABConfigurationRecord.TYPE.LIST;
						record.addValue("true", "Member");
						record.addValue("false", "Non-Member");

						if (members != null && members.contains(id)) {
							record.value = "true";
						} else {
							record.value = "false";
						}

						records.add(record);
					}
				}
			}

			return records;
		}

		return null;
	}

	@Override
	public void setConfiguration(String domain, List<OpenHABConfigurationRecord> records) {

	}

	@Override
	public String getCommonName() {
		return "ZWave";
	}

	@Override
	public String getDescription() {
		return "Provides interface to Z-Wave network";
	}

	@Override
	public void doAction(String domain, String action) {
		String[] splitDomain = domain.split("/");

		// There must be at least 2 components to the domain
		if (splitDomain.length < 2)
			return;

		if (splitDomain[0].equals("nodes")) {
			int nodeId = Integer.parseInt(splitDomain[1].substring(4));

			// Get the node - if it exists
			ZWaveNode node = zController.getNode(nodeId);
			if (node == null)
				return;

			ZWaveConfigurationCommandClass configurationCommandClass = (ZWaveConfigurationCommandClass) node
					.getCommandClass(CommandClass.CONFIGURATION);

			if (configurationCommandClass == null)
				return;

			if (splitDomain.length == 2) {
				if (action.equals("Save")) {
					// Write the node to disk
					ZWaveNodeSerializer nodeSerializer = new ZWaveNodeSerializer();
					nodeSerializer.SerializeNode(node);
				}

				// Return here as afterwards we assume there are more elements
				// in the domain array
				return;
			}

			if (splitDomain[2].equals("parameters")) {
				if (action.equals("Refresh")) {
					ZWaveProductDatabase database = new ZWaveProductDatabase();
					if (database.FindProduct(node.getManufacturer(), node.getDeviceType(), node.getDeviceId()) == false)
						return;

					List<ZWaveDbConfigurationParameter> configList = database.getProductConfigParameters();

					// Request all parameters for this node
					for (ZWaveDbConfigurationParameter parameter : configList)
						this.zController.sendData(configurationCommandClass.getConfigMessage(parameter.Index));
				}
			}

			if (splitDomain[2].equals("associations")) {
				if (action.equals("Refresh")) {
					ZWaveProductDatabase database = new ZWaveProductDatabase();
					if (database.FindProduct(node.getManufacturer(), node.getDeviceType(), node.getDeviceId()) == false)
						return;

					if (splitDomain.length == 3) {
						List<ZWaveDbAssociationGroup> groupList = database.getProductAssociationGroups();

						// Request all parameters for this node
						for (ZWaveDbAssociationGroup group : groupList)
							node.configAssociationReport(group.Index);
					} else if (splitDomain.length == 4) {
						int nodeArg = Integer.parseInt(splitDomain[3].substring(11));
						node.configAssociationReport(nodeArg);
					}
				}
			}
		}
	}

	@Override
	public void doSet(String domain, String value) {
		logger.debug("Set domain '{}' to '{}'", domain, value);
		String[] splitDomain = domain.split("/");

		// There must be at least 2 components to the domain
		if (splitDomain.length < 2)
			return;

		if (splitDomain[0].equals("nodes")) {
			int nodeId = Integer.parseInt(splitDomain[1].substring(4));

			ZWaveNode node = zController.getNode(nodeId);
			if (node == null)
				return;

			ZWaveConfigurationCommandClass configurationCommandClass = (ZWaveConfigurationCommandClass) node
					.getCommandClass(CommandClass.CONFIGURATION);

			if (configurationCommandClass == null)
				return;

			ZWaveProductDatabase database = new ZWaveProductDatabase();
			if (database.FindProduct(node.getManufacturer(), node.getDeviceType(), node.getDeviceId()) == false)
				return;

			if (splitDomain.length == 3) {
				if (splitDomain[2].equals("Name"))
					node.setName(value);
				if (splitDomain[2].equals("Location"))
					node.setLocation(value);
			} else if (splitDomain.length == 4) {
				if (splitDomain[2].equals("parameters")) {
					int paramIndex = Integer.parseInt(splitDomain[3].substring(13));
					List<ZWaveDbConfigurationParameter> configList = database.getProductConfigParameters();

					// Get the size
					int size = 1;
					for (ZWaveDbConfigurationParameter parameter : configList) {
						if (parameter.Index == paramIndex) {
							size = parameter.Size;
							break;
						}
					}

					logger.debug("Set parameter index '{}' to '{}'", paramIndex, value);

					ConfigurationParameter configurationParameter = new ConfigurationParameter(paramIndex,
							Integer.valueOf(value), size);
					this.zController.sendData(configurationCommandClass.setConfigMessage(configurationParameter));
				}
			} else if (splitDomain.length == 5) {
				if (splitDomain[2].equals("associations")) {
					int assocId = Integer.parseInt(splitDomain[3].substring(11));
					int assocArg = Integer.parseInt(splitDomain[4].substring(4));

					if (value.equalsIgnoreCase("true")) {
						logger.debug("Add association index '{}' to '{}'", assocId, assocArg);
						node.configAssociationAdd(assocId, assocArg);
					} else {
						logger.debug("Remove association index '{}' to '{}'", assocId, assocArg);
						node.configAssociationRemove(assocId, assocArg);
					}
				}
			}
		}
	}

	/**
	 * Event handler method for incoming Z-Wave events.
	 * 
	 * @param event
	 *            the incoming Z-Wave event.
	 */
	@Override
	public void ZWaveIncomingEvent(ZWaveEvent event) {

		// handle association class value events.
		if (event instanceof ZWaveAssociationEvent) {
			handleZWaveAssociationEvent((ZWaveAssociationEvent) event);
			return;
		}

	}

	/**
	 * Handle an incoming configuration parameter events The data is simply
	 * stored into the node for later use.
	 * 
	 * @param event
	 *            the incoming Z-Wave event.
	 */
	private void handleZWaveAssociationEvent(ZWaveAssociationEvent event) {
		logger.debug("Association received nodeId = {}, group = {}, new members = {}", new Object[] {
				event.getNodeId(), event.getGroup(), event.getMemberCnt() });

		// Find the node
		ZWaveNode node = zController.getNode(event.getNodeId());
		if (node == null) {
			logger.debug("Configuration parameter for nodeId {}. Node doesn't exist.", event.getNodeId());
			return;
		}

		// Add or update this parameter in the node class
		node.configAssociationAddMembers(event.getGroup(), event.getMembers());
	}

}
