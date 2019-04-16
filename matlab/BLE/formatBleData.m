function [bleData, allTime] = formatBleData(filename)
%format the BLE data from a file into a time-sorted list of scan data.
%To work properly, the file (filename i.e. file's path) should have its
%   parent folder in the format MM-DD-YY so that scans that are not from this
%   day's data are not considered
%Returned data is the time sorted strings for single instances of device
%scans, and their corresponding time of day in milliseconds, where 0 is midnight


%get the date from the filename/path; remove data that is not from today
slash=strfind(filename,'\');
%THIS FILE MUST BE IN A FOLDER WITH FORMAT 'MM-DD-YY'
date = filename(slash(end-1)+1:slash(end)-1);
date = strrep(date, '-', '/'); %replace dash with slash to match timestamp from file

%first, pull all information from the file
fid = fopen(filename, 'r');
res={}; %this contains all of the data
serial = fgetl(fid);

%extract all information from file
while ~feof(fid)
  line = fgetl(fid);
  %check to make sure the device scanned is from today (not holdover data
  %from past day)
  if(~strcmp(line(10:17), date))
      continue;
  end
  res{end+1,1} = line;
end
fclose(fid);

timeInit = getBleTimes(res{1});


%intialize some variables for the window's indices
winEndTime = 0;
winStartInd = 1;
%initialize data to be returned
bleData = cell(0); 
allTime = [];
numSorts = 0;

%used for debugging window sizes and data-save intervals
% largestWindowTime = 0;
% windowLengthTime = [];

for i=1:length(res)
    %need to separate the data into small windows, sort those, and the
    %combination of the sorted windows should  then _all_  be sorted in this way
    
    %select windows by comparing the last scan time of a device within the current window with the first scan
    %time of device scan 'i'. If it is past the latest scan time, then it
    %must have been saved in a different interval on the watch app, thus
    %marking the end of the window
    
    %res{i} %uncomment to print current line for debugging purposes
    
    %get all times device i was scanned at within this data-save interval
    times = getBleTimes(res{i});
    
    %if this condition is true, then we are past a window, and we need to
    %process said window, and setup for the next window
    if (date2num(times{1}) > winEndTime)
       winEndInd = i-1; %previous index was the end of the window
%        winEndTime = %is this needed?
       
       %SORT WINDOW HERE!!!
       if(winEndInd~=0) 
            %this ignores the first window, which is empty due to my methodology
            %select the window to be the lines (i.e. device scans) between
            %the end of the previous window, and start of next one
           [sortedWindow, sortedTime] = sortBleWindow(res(winStartInd:winEndInd));
           bleData = [bleData; sortedWindow];
           allTime = [allTime; sortedTime];
           
           %code to find how window length (in ms) for debugging the app, not
           %essential to processing data, so can stay commentedout
%            if(length(sortedTime)>0)
%                singleWindowLengthTime = sortedTime(end)-sortedTime(1);
%                if(singleWindowLengthTime>largestWindowTime)
%                   largestWindowTime=singleWindowLengthTime; 
%                end
%                windowLengthTime = [windowLengthTime, singleWindowLengthTime];
%            end
           
           numSorts = numSorts+1;
       end

       
       %init members for next window
       winStartTime = date2num(times{1});
       winStartInd = i; %marks the start index of the next window
       winEndTime = date2num(times{end});
        
    %if this condition is true, we need to push back the latest time contained in the window   
    elseif date2num(times{end}) > winEndTime
        winEndTime = date2num(times{end});
        
    end
       
end

%MAKE SURE TO HANDLE LAST WINDOW
[sortedWindow, sortedTime] = sortBleWindow(res(winStartInd:end));
bleData = [bleData; sortedWindow];
allTime = [allTime; sortedTime];


end
