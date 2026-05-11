package com.mealbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mealbot.dto.InbodyDto;
import com.mealbot.entity.Inbody;
import com.mealbot.entity.User;
import com.mealbot.repository.InbodyRepository;
import com.mealbot.util.InbodyCalculator;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InbodyService {

    private final InbodyRepository inbodyRepository;

    @Transactional
    public InbodyDto.InbodyResponse save(User user, InbodyDto.SaveRequest request) {
        Inbody.Gender gender = parseGender(request.getGender());
        int bmr = InbodyCalculator.calcBmr(request.getHeight(), request.getWeight(), request.getAge(), gender);
        Inbody inbody = Inbody.builder()
                .user(user)
                .height(request.getHeight())
                .weight(request.getWeight())
                .age(request.getAge())
                .gender(gender)
                .activityLevel(request.getActivityLevel())
                .skeletalMuscle(request.getSkeletalMuscle())
                .bodyFat(request.getBodyFat())
                .bodyFatPercent(request.getBodyFatPercent())
                .bmi(InbodyCalculator.calcBmi(request.getHeight(), request.getWeight()))
                .bmr(bmr)
                .dailyCalories(InbodyCalculator.calcDailyCalories(bmr, request.getActivityLevel()))
                .protein(request.getProtein())
                .mineral(request.getMineral())
                .bodyWater(request.getBodyWater())
                .visceralFat(request.getVisceralFat())
                .build();

        inbodyRepository.save(inbody);
        return toResponse(inbody);
    }

    public List<InbodyDto.InbodyResponse> getList(User user) {
        return inbodyRepository.findByUserOrderByMeasuredAtDesc(user).stream()
                .map((inbody) -> this.toResponse(inbody))
                .toList();
    }

    @Transactional
    public void delete(User user, Long inbodyId) {
        Inbody inbody = inbodyRepository.findByIdAndUser(inbodyId, user)
                .orElseThrow(() -> new IllegalArgumentException("인바디 기록을 찾을 수 없습니다: " + inbodyId));
        inbodyRepository.delete(inbody);
    }

    // "남성" → MALE, "여성" → FEMALE
    private Inbody.Gender parseGender(String gender) {
        if ("남성".equals(gender)) return Inbody.Gender.MALE;
        if ("여성".equals(gender)) return Inbody.Gender.FEMALE;
        throw new IllegalArgumentException("올바르지 않은 성별 값입니다: " + gender);
    }

    private InbodyDto.InbodyResponse toResponse(Inbody inbody) {
        String genderLabel = inbody.getGender() == Inbody.Gender.MALE ? "남성" : "여성";
        return new InbodyDto.InbodyResponse(
                inbody.getId(),
                inbody.getHeight(),
                inbody.getWeight(),
                inbody.getAge(),
                genderLabel,
                inbody.getSkeletalMuscle(),
                inbody.getBodyFat(),
                inbody.getBodyFatPercent(),
                inbody.getBmi(),
                inbody.getBmr(),
                inbody.getDailyCalories(),
                inbody.getProtein(),
                inbody.getMineral(),
                inbody.getBodyWater(),
                inbody.getVisceralFat(),
                inbody.getMeasuredAt()
        );
    }
}