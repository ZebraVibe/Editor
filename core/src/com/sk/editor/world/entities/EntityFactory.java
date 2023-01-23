package com.sk.editor.world.entities;

import com.artemis.World;

public class EntityFactory {

	private World world;
	
	public EntityFactory(World world) {
		this.world = world;
	}
	
	public <T extends Enum<?>> void create(T entityType){
		create(entityType.toString());
	}
	
	public int create(String entityType) {
		int eid = world.create();
		
		if(entityType == null)return eid;
		entityType = entityType.toLowerCase();
		
		switch (entityType) {
		default: // case "empty"
			return eid;
		}
	}
	
}
