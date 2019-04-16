function [times, fullData] = unrollBleDevices(bleStr)
%A single BLE device scan often contains many timestamps corresponding to
%different scans within a several minute period. This function unrolls
%those so that each line of 'fullData' is a single instance of a scan for a
%single device

timeStr = getBleTimes(bleStr);
rssi = getBleRssi(bleStr);

numScans = size(timeStr,1);
times = zeros(numScans,1);
fullData = cell(numScans,1);

brackets_start = strfind(bleStr,'{');
brackets_end = strfind(bleStr,'}');

%get the static data (not dependent on number of scans)
%gets string := mac:{xx:xx:xx:xx:xx:xx},name:{NAME}
macAndName = bleStr(brackets_end(1)+2:brackets_end(3));
%gets string := raw_data:{LONG HEX STRING}
rawData = bleStr(brackets_end(end-1)+2:end);

%unroll the string to include many strings, one timestamp each
for i=1:numScans
    str = strcat('time:{',timeStr{i},'},');
    str = strcat(str,macAndName,',rssi:{',rssi{i},'},');
    str = strcat(str,rawData);

    fullData{i} = str;
    times(i) = date2num(timeStr{i});
    
end
end