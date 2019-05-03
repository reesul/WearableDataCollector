function [sortedData, sortedTimes] = sortBleWindow(BleWindowStrs)
%Sort a window of BLE data; return the data and times

numDevices = size(BleWindowStrs,1);
unsortedData = cell(0);
unsortedTimes = [];

%first, unroll the data, get into a cell arrays (unsorted)
for i=1:numDevices
   bleStr = BleWindowStrs{i};
   [times, data] = unrollBleDevices(bleStr);
    unsortedTimes = [unsortedTimes; times];
    unsortedData = [unsortedData; data];
    
end

%get all times from the data, and use these numbers to sort
%   Use the indexes to rewrite the unsortedData to match the sorted Times
[sortedTimes, index] = sort(unsortedTimes, 'ascend');

sortedData = unsortedData(index);

end