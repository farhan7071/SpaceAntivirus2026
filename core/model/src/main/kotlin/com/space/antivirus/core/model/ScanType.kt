package com.space.antivirus.core.model

/** What kind of scan a ScanSession represents. Shape only — the actual
 *  enumeration/analysis logic for each type is out of scope for Sprint
 *  004A (domain layer only). */
enum class ScanType {
    QUICK,
    FULL,
    CUSTOM,
}
