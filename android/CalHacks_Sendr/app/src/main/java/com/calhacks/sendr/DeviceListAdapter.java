package com.calhacks.sendr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

public class DeviceListAdapter extends ArrayAdapter<DeviceListDataClass>
{
    private List<DeviceListDataClass> deviceList;
    private Context context;

    public DeviceListAdapter(List<DeviceListDataClass> deviceList, Context context) {
        super(context, R.layout.device_list_view_text, deviceList);
        this.deviceList = deviceList;
        this.context = context;
    }

    private static class DeviceDataHolder {
        public TextView nameTextView;
        public CheckBox checkBox;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        DeviceDataHolder holder = new DeviceDataHolder();

        if(convertView == null) {

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.device_list_view_text, null);

            holder.nameTextView = (TextView) v.findViewById(R.id.name_list_text);
            holder.checkBox = (CheckBox) v.findViewById(R.id.check_box);

            holder.checkBox.setOnCheckedChangeListener((SelectDeviceToSend) context);

        } else {
            holder = (DeviceDataHolder) v.getTag();
        }

        DeviceListDataClass device = deviceList.get(position);
        holder.nameTextView.setText(device.getName() + "\t\t\t\t(" + device.getDevice() + ")");
        holder.checkBox.setChecked(device.isSelected());
        holder.checkBox.setTag(device);

        return v;
    }
}
