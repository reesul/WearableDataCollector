function [times] = getBleTimes(bleStr)
%get individual time of scan for a single device's BLE entry in data from
%the smart watch data-collection app

    
    %get the times
    brackets_start = strfind(bleStr,'{');
    brackets_end = strfind(bleStr,'}');
    
    timestr = bleStr(brackets_start(1)+1:brackets_end(1)-1);
    
    semicolons = strfind(timestr,';');
    times = cell(length(semicolons)+1,1);
    if length(times)==1
        times{1} = timestr;
    else
        times{1} = timestr(1:semicolons(1)-1);
        for j = 2:length(semicolons)
            times{j} = timestr(semicolons(j-1)+1:semicolons(j)-1);
        end
        times{end} = timestr(semicolons(end)+1:end);
    end



end
