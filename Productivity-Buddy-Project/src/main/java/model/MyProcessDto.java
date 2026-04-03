package model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MyProcessDto {
    private String originalName;

    private String aliasName;

    private String category;

    private boolean isTrackingFreezed;

    private long totalTimeSeconds;



}
