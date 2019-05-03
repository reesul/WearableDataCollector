function rssi = getBleRssi(bleStr)
%Get all RSSI values for a single devices BLE scans
%data returned in a cell array

    
    %get the times
    brackets_start = strfind(bleStr,'{');
    brackets_end = strfind(bleStr,'}');
    
    rssiStr = bleStr(brackets_start(end-1)+1:brackets_end(end-1)-1);
    
    semicolons = strfind(rssiStr,';');
    rssi = cell(length(semicolons)+1,1);
    if length(rssi)==1
        rssi{1} = rssiStr;
    else
        rssi{1} = rssiStr(1:semicolons(1)-1);
        for j = 2:length(semicolons)
            rssi{j} = rssiStr(semicolons(j-1)+1:semicolons(j)-1);
        end
        rssi{end} = rssiStr(semicolons(end)+1:end);
    end





end