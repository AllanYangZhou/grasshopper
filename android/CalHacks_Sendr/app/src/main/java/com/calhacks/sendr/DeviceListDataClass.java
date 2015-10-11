package com.calhacks.sendr;

class DeviceListDataClass
{
    private String name;
    private String device;
    private String uid;
    private boolean selected = false;

    public DeviceListDataClass(String name, String device, String uid) {
        super();
        this.name = name;
        this.device = device;
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getUID()
    {
        return uid;
    }

    public void setUID(String uid)
    {
        this.uid = uid;
    }
}
