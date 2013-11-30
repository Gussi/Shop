package com.snowgears.shop;

public enum ShopType {

    SELLING(0),
    
    BUYING(1),
    
    BARTER(2);
    
    private final int slot;

    private ShopType(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }
    
    public String getName(){
    	if(this == ShopType.SELLING)
    		return "selling";
    	else if(this == ShopType.BUYING)
    		return "buying";
    	else
    		return "barter";
    }
}