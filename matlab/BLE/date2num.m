function [num] = date2num(date)
%convert date string into a number (in milliseconds). Can handle two
%formats: dd/mm/yy HH:mm:ss:SSS OR dd/mm/yy HH:mm:ss
%num=0 corresponds to 00:00:00.000
%NOTE: converted number is the time of day - does not account for date

num=0; %in ms
if length(date) == 21
    %       hours                       minutes
    num = ((str2double(date(10:11))*60+str2double(date(13:14)))*60 + str2double(date(16:17)))*1000 + str2double(date(19:end));
    
else %format is dd/mm/yy HH:mm:ss
    
    num = ((str2double(date(10:11))*60+str2double(date(13:14)))*60 + str2double(date(16:17)))*1000;
    
end

end