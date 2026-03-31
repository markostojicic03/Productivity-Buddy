package model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class MyProcessDto {
    private String originalName;

    private String aliasName;

    private String category;

    private boolean isTrackingFreezed;

    private long totalTimeSeconds;



}
