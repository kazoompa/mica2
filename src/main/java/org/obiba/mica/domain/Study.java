package org.obiba.mica.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateDeserializer;
import org.obiba.mica.domain.util.CustomLocalDateSerializer;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * A Study.
 */
@Entity
@Table(name = "T_STUDY")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Study implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private long id;

    @Size(min = 1, max = 50)
    @Column(name = "sample_text_attribute")
    private String sampleTextAttribute;

    @NotNull
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDate")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = CustomLocalDateSerializer.class)
    @Column(name = "sample_date_attribute")
    private LocalDate sampleDateAttribute;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSampleTextAttribute() {
        return sampleTextAttribute;
    }

    public void setSampleTextAttribute(String sampleTextAttribute) {
        this.sampleTextAttribute = sampleTextAttribute;
    }

    public LocalDate getSampleDateAttribute() {
        return sampleDateAttribute;
    }

    public void setSampleDateAttribute(LocalDate sampleDateAttribute) {
        this.sampleDateAttribute = sampleDateAttribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Study study = (Study) o;

        if (id != study.id) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "Study{" +
                "id=" + id +
                ", sampleTextAttribute='" + sampleTextAttribute + '\'' +
                ", sampleDateAttribute=" + sampleDateAttribute +
                '}';
    }
}
