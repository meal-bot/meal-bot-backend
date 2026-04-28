package pack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inbody")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inbody {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 기본 정보 (필수)
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal height;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal weight;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private Gender gender;

    // 상세 측정값 (선택)
    @Column(precision = 4, scale = 1)
    private BigDecimal skeletalMuscle;

    @Column(precision = 4, scale = 1)
    private BigDecimal bodyFat;

    @Column(precision = 4, scale = 1)
    private BigDecimal bodyFatPercent;

    @Column(precision = 4, scale = 1)
    private BigDecimal bmi;

    @Column(precision = 4, scale = 1)
    private BigDecimal protein;

    @Column(precision = 4, scale = 1)
    private BigDecimal mineral;

    @Column(precision = 4, scale = 1)
    private BigDecimal bodyWater;

    private Integer visceralFat;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime measuredAt;

    public enum Gender {
        MALE, FEMALE
    }
}