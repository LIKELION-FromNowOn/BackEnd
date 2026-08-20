package com.youin.now.checkin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

/**
 * {@code checkin_signals} 테이블 — 그날 고른 이상 징후 하나.
 *
 * <p><b>마스터 징후와 직접 입력을 한 테이블에 담습니다.</b>
 * 마스터를 고르면 {@code signalId} 가 차고, 직접 적으면 {@code customText} 가 찹니다.
 * 둘 중 하나는 반드시 있어야 합니다({@code ck_checkin_signals_one}).
 *
 * <p><b>직접 입력은 저장 전에 위기 신호 검사를 통과해야 합니다.</b>
 * {@code SafetyPort.check(text, SIGNAL_CUSTOM)} 을 먼저 부르십시오 — 프롬프트로 막지 않습니다.
 */
@Entity
@Table(name = "checkin_signals")
public class CheckinSignal {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "checkin_id", nullable = false)
    private String checkinId;

    /** 마스터 징후 번호. 직접 입력이면 null */
    @Column(name = "signal_id")
    private String signalId;

    /** 직접 적은 징후. 마스터를 고른 경우 null. <b>최대 5개</b> */
    @Column(name = "custom_text")
    private String customText;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    private OffsetDateTime createdAt;

    protected CheckinSignal() {}

    private CheckinSignal(String id, String checkinId, String signalId, String customText) {
        this.id = id;
        this.checkinId = checkinId;
        this.signalId = signalId;
        this.customText = customText;
    }

    public static CheckinSignal ofMaster(String id, String checkinId, String signalId) {
        return new CheckinSignal(id, checkinId, signalId, null);
    }

    public static CheckinSignal ofCustom(String id, String checkinId, String text) {
        return new CheckinSignal(id, checkinId, null, text);
    }

    public String id()         { return id; }
    public String checkinId()  { return checkinId; }
    public String signalId()   { return signalId; }
    public String customText() { return customText; }
}
