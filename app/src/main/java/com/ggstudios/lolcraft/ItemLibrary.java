package com.ggstudios.lolcraft;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

public class ItemLibrary {
	private SparseArray<ItemInfo> itemDictionary;
	private List<ItemInfo> items;
	private List<ItemInfo> purchasableItems;
	private Object itemListLock = new Object();

	public ItemLibrary() {}

	public void initialize(List<ItemInfo> list) {
		synchronized(itemListLock) {
			items = list;
			
			purchasableItems = new ArrayList<ItemInfo>();

			itemDictionary = new SparseArray<ItemInfo>();
			for (ItemInfo i : items) {
				itemDictionary.put(i.id, i);
				
				// We will define a purchasable item to be an item purchasable 
				// in summoner's rift only...
				if (i.purchasable) {
					purchasableItems.add(i);
				}
			}
		}
	}

	public List<ItemInfo> getAllItemInfo() {
		synchronized(itemListLock) {
			return items;
		}
	}
	
	public List<ItemInfo> getPurchasableItemInfo() {
		synchronized(itemListLock) {
			return purchasableItems;
		}
	}

	public ItemInfo getItemInfo(int itemId) {
		return itemDictionary.get(itemId);
	}
}
