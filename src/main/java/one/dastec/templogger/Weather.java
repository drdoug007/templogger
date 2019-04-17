package one.dastec.templogger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Weather {

    private float temp;
    private float humidity;
    private float heatIndex;
}