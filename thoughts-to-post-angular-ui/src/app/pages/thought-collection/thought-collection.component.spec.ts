import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ThoughtCollectionComponent } from './thought-collection.component';
import { ThoughtsService } from '../../services/thoughts.service';
import { of } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

describe('ThoughtCollectionComponent', () => {
  let component: ThoughtCollectionComponent;
  let fixture: ComponentFixture<ThoughtCollectionComponent>;
  let thoughtsServiceSpy: jasmine.SpyObj<ThoughtsService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const tSpy = jasmine.createSpyObj('ThoughtsService', ['getFullCategories', 'generateSearchCriteria', 'executeSearch']);
    const rSpy = jasmine.createSpyObj('Router', ['navigate']);

    tSpy.getFullCategories.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [ThoughtCollectionComponent, CommonModule, FormsModule],
      providers: [
        { provide: ThoughtsService, useValue: tSpy },
        { provide: Router, useValue: rSpy }
      ]
    }).compileComponents();

    thoughtsServiceSpy = TestBed.inject(ThoughtsService) as jasmine.SpyObj<ThoughtsService>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    fixture = TestBed.createComponent(ThoughtCollectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should generate search string', () => {
    component.selectedCategory = { category: 'Tech', searchDescription: '', modelRole: '' };
    component.collectionDescription = 'AI';
    thoughtsServiceSpy.generateSearchCriteria.and.returnValue(of('suggested query'));

    component.generateSearchString();

    expect(component.suggestedSearchString).toBe('suggested query');
    expect(thoughtsServiceSpy.generateSearchCriteria).toHaveBeenCalled();
  });
});
