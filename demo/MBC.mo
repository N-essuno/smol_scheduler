model LinearInOut
  model LinearWater
    input Real i;
    parameter Real des = 0.000030003;
    output Real value;
  initial equation
   value = i;
  equation
    der(value) = -des; //calibrated using the original phyiscal setup: -0.000030003
  end LinearWater;
  LinearWater ioi;
equation
  ioi.i = 1;
end LinearInOut;